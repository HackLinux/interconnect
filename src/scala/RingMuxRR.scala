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
  * Mux for the ring with round-robin (fair) arbitration
  * 
  * The special property of the ring mux is that is preserves NoC
  * worms (packets) and does not interleave flits from both ports.
  */
class RingMuxRR(data_width: Int)
    extends Module {

  // Two input ports and one output port
  val io = new Bundle {
    val in0 = new NetworkLink(data_width).flip()
    val in1 = new NetworkLink(data_width).flip()
    val out = new NetworkLink(data_width)
  }

  // Extract flit information
  val in0_type = io.in0.bits(data_width + 1, data_width)
  val in1_type = io.in1.bits(data_width + 1, data_width)
  val in0_is_first = in0_type(1) === Bits(1)
  val in1_is_first = in1_type(1) === Bits(1)
  val in0_is_last = in0_type(0) === Bits(1)
  val in1_is_last = in1_type(0) === Bits(1)

  // State machine. We store the information which port was served
  // before in the state so that we can make the fair decision even
  // for concurrently arriving packets after a few cycles. This is
  // absolutely fair then.
  val s_noworm_prio0 :: s_noworm_prio1 :: s_in0 :: s_in1 :: Nil = Enum(Bits(), 4)
  val nxt_active = UInt()
  val active = Reg(UInt(), nxt_active, s_noworm_prio0)

  switch(active) {
    // Default output and keep state
    nxt_active := active
    io.out.valid := Bool(false)
    io.out.bits := Bits(0)
    io.in0.ready := Bool(false)
    io.in1.ready := Bool(false)

    is(s_noworm_prio0) {
      // We are not in a worm and port 0 has priority
      when(io.in0.valid && in0_is_first) {
        // Forward this port
        io.in0.ready := io.out.ready
        io.out.valid := io.in0.valid
        io.out.bits := io.in0.bits

        when(io.out.ready) {
          // when we have an actual transfer
          when(!in0_is_last) {
            // Enter worm
            nxt_active := s_in0
          } .otherwise {
            // Single flit packet, next priority to
            nxt_active := s_noworm_prio1
          }
        }
      } .elsewhen(io.in1.valid && in1_is_first) {
        // Forward this port if port 0 doesn't want to
        io.in1.ready := io.out.ready
        io.out.valid := io.in1.valid
        io.out.bits := io.in1.bits

        when(!in1_is_last) {
          // Enter worm, we can safely transfer to the worm handling
          // even if there was not transfer this cycle
          nxt_active := s_in1
        }
      }
    }
    is(s_noworm_prio1) {
      // We are not in a worm and port 1 has priority
      when(io.in1.valid && in1_is_first) {
        // Forward this port
        io.in1.ready := io.out.ready
        io.out.valid := io.in1.valid
        io.out.bits := io.in1.bits

        when(io.out.ready) {
          // when we have an actual transfer
          when(!in1_is_last) {
            // Enter worm
            nxt_active := s_in1
          } .otherwise {
            // Single flit packet, next priority to
            nxt_active := s_noworm_prio0
          }
        }
      } .elsewhen(io.in0.valid && in0_is_first) {
        // Forward this port if port 1 doesn't want to
        io.in0.ready := io.out.ready
        io.out.valid := io.in0.valid
        io.out.bits := io.in0.bits

        when(!in0_is_last) {
          // Enter worm, we can safely transfer to the worm handling
          // even if there was not transfer this cycle
          nxt_active := s_in1
        }
      }
    }
    is(s_in0) {
      // Forward worm
      io.in0.ready := io.out.ready
      io.out.valid := io.in0.valid
      io.out.bits := io.in0.bits

      when(io.in0.valid && io.out.ready && in0_is_last) {
        // If this was the last transfer, arbitrate again and give
        // priority to the other port
        nxt_active := s_noworm_prio1
      }
    }
    is(s_in1) {
      // Forward worm
      io.in1.ready := io.out.ready
      io.out.valid := io.in1.valid
      io.out.bits := io.in1.bits

      when(io.in1.valid && io.out.ready && in1_is_last) {
        // If this was the last transfer, arbitrate again and give
        // priority to the other port
        nxt_active := s_noworm_prio0
      }
    }
  }

}

/**
  * Tester for RingMuxRR
  */
class RingMuxRRTests(c: RingMuxRR) extends Tester(c) {
  /* STEP 0 */
  // Check combinational correctness in noworm state
  // Output is ready, no request
  poke(c.io.in0.valid, 0)
  poke(c.io.in1.valid, 0)
  poke(c.io.out.ready, 0)
  expect(c.io.in0.ready, 0)
  expect(c.io.in1.ready, 0)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 1 */
  // Start worm at port 0, out not ready
  poke(c.io.in0.valid, 1)
  poke(c.io.in0.bits, 0x2dead)
  expect(c.io.in0.ready, 0)
  expect(c.io.in1.ready, 0)
  expect(c.io.out.valid, 1)
  step(1)
  /* STEP 2 */
  // out is ready now, transfer
  poke(c.io.out.ready, 1)
  expect(c.io.in0.ready, 1)
  expect(c.io.in1.ready, 0)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x2dead)
  step(1)
  /* STEP 3 */
  // Pause in0, start in1, no transfer
  poke(c.io.in0.valid, 0)
  poke(c.io.in1.bits, 0x2abc1)
  poke(c.io.in1.valid, 1)
  expect(c.io.in0.ready, 1)
  expect(c.io.in1.ready, 0)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 4 */
  // Finish in0
  poke(c.io.in0.valid, 1)
  poke(c.io.in0.bits, 0x1beef)
  expect(c.io.in0.ready, 1)
  expect(c.io.in1.ready, 0)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x1beef)
  step(1)
  /* STEP 5 */
  // new worm of in1 transferred
  poke(c.io.in0.valid, 0)
  expect(c.io.in0.ready, 0)
  expect(c.io.in1.ready, 1)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x2abc1)
  step(1)
  /* STEP 6 */
  // new worm at in0
  // worm of in1 completed
  poke(c.io.in0.valid, 1)
  poke(c.io.in0.bits, 0x3abc0)
  poke(c.io.in1.valid, 1)
  poke(c.io.in1.bits, 0x1dead)
  expect(c.io.in0.ready, 0)
  expect(c.io.in1.ready, 1)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x1dead)
  step(1)
  /* STEP 7 */
  // Single flit packets at in0 and in1
  // in0 has precedence as in1 was served
  poke(c.io.in1.bits, 0x3abc1)
  expect(c.io.in0.ready, 1)
  expect(c.io.in1.ready, 0)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x3abc0)
  step(1)
  /* STEP 8 */
  // Single flit packets at in0 and in1
  // in1 has precedence, round-robin
  poke(c.io.in0.bits, 0x3abc2)
  expect(c.io.in0.ready, 0)
  expect(c.io.in1.ready, 1)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x3abc1)
  step(1)
  /* STEP 9 */
  // Single flit packets at in0 and in1
  // in0 has precedence, round-robin
  poke(c.io.in1.bits, 0x3abc3)
  expect(c.io.in0.ready, 1)
  expect(c.io.in1.ready, 0)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x3abc2)
  step(1)
  /* STEP 10 */
  // in1 transfer
  poke(c.io.in0.valid, 0)
  expect(c.io.in0.ready, 0)
  expect(c.io.in1.ready, 1)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x3abc3)
  step(1)
}
