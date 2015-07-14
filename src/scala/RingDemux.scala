/* Copyright (c) 2015 by the author(s)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * =============================================================================
 *
 * Author(s):
 *   Stefan Wallentowitz <stefan@wallentowitz.de>
 */

package OpenSoCDebug

import Chisel._

/**
  * Ring demuxer
  *
  * The ring demuxer is a stateful demux module, that demuxes worms
  * (packets) and not single data units (flits). Based on the header
  * routing information the entire worm is forwarded to one of the two
  * output ports: Port 0 if the parameter id matches the destination
  * or port 1 otherwise.
  */

class RingDemux(data_width: Int, id: UInt, dest_width: Int)
    extends Module {
  // One input and two output ports (decoupled I/O with data_width + 2
  // bits for the flit type)
  val io = new Bundle {
    val in = new NetworkLink(data_width).flip()
    val out0 = new NetworkLink(data_width)
    val out1 = new NetworkLink(data_width)
  }

  /* Extract input flit information */
  // Type are the two extra bits
  val in_type = io.in.bits(data_width + 1, data_width)
  val in_is_first = in_type(1) === Bits(1) // First flit in packet
  val in_is_last = in_type(0) === Bits(1)  // Last flit in packet

  // Destination is only a valid field iff in_is_first
  val in_dest = io.in.bits(6 + dest_width, 6)
  // Selection conditions for output ports
  val in_out0 = in_dest === UInt(id)
  val in_out1 = !in_out0

  /* State machine and output */
  // Enum that holds the 3 states
  val s_noworm :: s_worm0 :: s_worm1 :: Nil = Enum(Bits(), 3)

  // Combinational signal for the state register
  val nxt_in_worm = UInt()
  // State register
  val in_worm = Reg(UInt(), nxt_in_worm, s_noworm)

  // We always forward the bits on both ports, valid select correct
  // one
  io.out0.bits := io.in.bits
  io.out1.bits := io.in.bits

  // State transitions and output
  switch(in_worm) {
    // Default values
    nxt_in_worm := in_worm // keep state
    io.out0.valid := Bool(false) // No output
    io.out1.valid := Bool(false) // No output
    io.in.ready := Bool(false) // Don't accept input

    // (reset state) we are not currently in a worm, meaning this is
    // no tail part of a packet after the first flit. We therefore
    // select the proper output for incoming packets and enter the
    // proper worm state.
    is(s_noworm) {
      // Signal ready to the input from the correct input port (mux
      // ready by destination)
      io.in.ready := (in_out0 & io.out0.ready) |
          (in_out1 & io.out1.ready)

      // Only if the input is valid
      when (io.in.valid) {
        // This must be a first flit
        // TODO: assert(in_is_first)

        // Forward the valid signal to the proper output
        io.out0.valid := in_out0.toUInt()
        io.out1.valid := in_out1.toUInt()

        // Only when this is not the last flit in the packet we need
        // to enter a worm state, otherwise the next flit will be the
        // first of a new packet
        when (!in_is_last) {
          // Set the proper next state depending on the destination
          // (we can move safely to the new state even if the flit is
          // not actually transferred this cycle (in.valid &&
          // outx.ready)
          when (in_out0 && io.out0.ready) {
            nxt_in_worm := s_worm0
          } .elsewhen(in_out1 && io.out1.ready) {
            nxt_in_worm := s_worm1
          }
        }
      }
    }

    // We currently route a worm to output 0
    is(s_worm0) {
      // Forward ready and valid in both directions
      io.in.ready := io.out0.ready
      io.out0.valid := io.in.valid

      // If this is the last in a packet, go back and check next
      // packet
      when (in_is_last && io.out0.ready && io.in.valid) {
        nxt_in_worm := s_noworm
      }
    }

    // We currently route a worm to output 1
    is(s_worm1) {
      // Forward ready and valid in both directions
      io.in.ready := io.out1.ready
      io.out1.valid := io.in.valid

      // If this is the last in a packet, go back and check next
      // packet
      when (in_is_last && io.out1.ready && io.in.valid) {
        nxt_in_worm := s_noworm
      }
    }

  }
}

class RingDemuxTests(c: RingDemux) extends Tester(c) {
  // Check combinational correctness in noworm state
  // Not output is ready, header flit for out0
  poke(c.io.in.valid, 1)
  poke(c.io.in.bits, 0x20000)
  poke(c.io.out0.ready, 0)
  poke(c.io.out1.ready, 0)
  expect(c.io.out0.valid, 1)
  expect(c.io.out1.valid, 0)
  step(1)
  // Check proper forwarding to out0 when it becomes ready
  poke(c.io.out0.ready, 1)
  expect(c.io.in.ready, 1)
  expect(c.io.out0.valid, 1)
  expect(c.io.out1.valid, 0)
  step(1)
  // Transfer a data flit
  poke(c.io.in.bits, 0x0dead)
  expect(c.io.out0.bits, 0x0dead)
  expect(c.io.out0.valid, 1)
  expect(c.io.out1.valid, 0)
  step(1)
  // Transfer the last data flit
  poke(c.io.in.bits, 0x1beef)
  expect(c.io.out0.bits, 0x1beef)
  expect(c.io.out0.valid, 1)
  expect(c.io.out1.valid, 0)
  step(1)
  // Pause transfer, both ready, no in
  poke(c.io.in.valid, 0)
  poke(c.io.out0.ready, 1)
  poke(c.io.out1.ready, 1)
  expect(c.io.out0.valid, 0)
  expect(c.io.out1.valid, 0)
  step(1)
  // Transfer a single flit
  poke(c.io.in.valid, 1)
  poke(c.io.in.bits, 0x3abcd)
  expect(c.io.out0.bits, 0x3abcd)
  expect(c.io.out0.valid, 0)
  expect(c.io.out1.valid, 1)
}
