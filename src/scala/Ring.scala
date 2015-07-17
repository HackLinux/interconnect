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
  * Debug ring
  * 
  * This is the ring interconnect for the debug subsystem. The default
  * is a data width of 16, which can be changed with the data_width
  * parameter. Maximum 1024 nodes can be connected, where this can be
  * changed with the dest_width parameter (default: 10). Select the
  * number of ports with the num_ports parameter.
  * 
  * The routers are output buffered and buffer_depth is used to set
  * the number of entries in the buffers.
  * 
  * The ring is deadlock-free with two physical rings forming a
  * spiral. This implementation favors connecting the central element
  * at the last port of the ring as it uses the outer ring to send
  * data to the other participants and the inner ring is used for
  * traces to the host interface.
  */
class Ring (num_ports: Int, data_width: Int = 16, dest_width: Int = 10,
  buffer_depth : Int = 4)
    extends Module {
  val io = new Bundle {
    val in = Vec.fill(num_ports) { new NetworkLink(data_width).flip() }
    val out = Vec.fill(num_ports) { new NetworkLink(data_width) }
  }

  assert(Bool(log2Up(num_ports) <= dest_width), "Too many ring ports")

  val routers = Vec.fill(num_ports){
    Module(new RingRouter(data_width = data_width, dest_width = dest_width,
      buffer_depth = buffer_depth)).io
  }


  for(i <- 0 until num_ports) {
    routers(i).id := UInt(i)
    routers(i).in <> io.in(i)
    routers(i).out <> io.out(i)
  }

  for(i <- 0 until (num_ports-1)) {
    routers(i).ring_out0 <> routers(i+1).ring_in0
    routers(i).ring_out1 <> routers(i+1).ring_in1
  }

  routers(0).ring_in1 <> routers(num_ports-1).ring_out0
  routers(0).ring_in0.valid := UInt(0)
  routers(0).ring_in0.bits := UInt(0)
  routers(num_ports-1).ring_out1.ready := UInt(0)
}

class RingTests(c: Ring) extends Tester(c) {
  // Sinks 0 and 2 are ready
  poke(c.io.out(0).ready, 1)
  poke(c.io.out(2).ready, 1)
  /* STEP 0 */
  // Send from 3 to 2 and from 1 to 0
  // This is an example that would deadlock
  poke(c.io.in(1).valid, 1)
  poke(c.io.in(1).bits, 0x20000)
  poke(c.io.in(3).valid, 1)
  poke(c.io.in(3).bits, 0x20080)
  expect(c.io.in(1).ready, 1)
  expect(c.io.in(3).ready, 1)
  step(1)
  /* STEP 1 */
  poke(c.io.in(1).bits, 0x01111)
  poke(c.io.in(3).bits, 0x01234)
  expect(c.io.in(1).ready, 1)
  expect(c.io.in(3).ready, 1)
  step(1)
  /* STEP 2 */
  poke(c.io.in(1).bits, 0x02222)
  poke(c.io.in(3).bits, 0x05678)
  expect(c.io.in(1).ready, 1)
  expect(c.io.in(3).ready, 1)
  step(1)
  /* STEP 3 */
  poke(c.io.in(1).bits, 0x03333)
  poke(c.io.in(3).bits, 0x09abc)
  expect(c.io.in(1).ready, 1)
  expect(c.io.in(3).ready, 1)
  step(1)
  /* STEP 4 */
  poke(c.io.in(1).bits, 0x14444)
  poke(c.io.in(3).bits, 0x1def0)
  expect(c.io.in(1).ready, 1)
  expect(c.io.in(3).ready, 1)
  expect(c.io.out(0).valid, 0)
  expect(c.io.out(2).valid, 1)
  expect(c.io.out(2).bits, 0x20080)
  step(1)
  /* STEP 5 */
  poke(c.io.in(1).valid, 0)
  poke(c.io.in(3).valid, 0)
  expect(c.io.out(0).valid, 0)
  expect(c.io.out(2).valid, 1)
  expect(c.io.out(2).bits, 0x01234)
  step(1)
  /* STEP 6 */
  expect(c.io.out(0).valid, 0)
  expect(c.io.out(2).valid, 1)
  expect(c.io.out(2).bits, 0x05678)
  step(1)
  /* STEP 7 */
  expect(c.io.out(0).valid, 1)
  expect(c.io.out(0).bits, 0x20000)
  expect(c.io.out(2).valid, 1)
  expect(c.io.out(2).bits, 0x09abc)
  step(1)
  /* STEP 8 */
  expect(c.io.out(0).valid, 1)
  expect(c.io.out(0).bits, 0x01111)
  expect(c.io.out(2).valid, 1)
  expect(c.io.out(2).bits, 0x1def0)
  step(1)
  /* STEP 9 */
  expect(c.io.out(0).valid, 1)
  expect(c.io.out(0).bits, 0x02222)
  expect(c.io.out(2).valid, 0)
  step(1)
  /* STEP 10 */
  expect(c.io.out(0).valid, 1)
  expect(c.io.out(0).bits, 0x03333)
  expect(c.io.out(2).valid, 0)
  step(1)
  /* STEP 11 */
  expect(c.io.out(0).valid, 1)
  expect(c.io.out(0).bits, 0x14444)
  expect(c.io.out(2).valid, 0)
  step(1)
  /* STEP 12 */
  expect(c.io.out(0).valid, 0)
  expect(c.io.out(1).valid, 0)
  expect(c.io.out(2).valid, 0)
  expect(c.io.out(3).valid, 0)
  step(1)
}
