package OpenSoCDebug

import Chisel._

class NetworkLink (data_width: Int)
    extends DecoupledIO(Bits(width=data_width+2)) {

}

/** Ring Router
  * 
  * This is the basic building block for the debug interconnect
  * ring. The router is very straight-forward and is designed for
  * deadlock-free rings: It has two ring paths to build a circle-free
  * channel dependency diagram (spiral). In the ring the input and
  * output ids are simply connected (router(i).out0 <> router(i+1).in0
  * and router(i).out1 <> router(i+1).in1). After the last router, the
  * inner ring (0) is connected (router(N-1).out0 <> router(0).in1)
  * and the remaining input and output are unconnected.
  *
  * The router is output buffered and the buffer size is
  * parametrizable. It also has an ID and all packets with the
  * matching destination are ejected to the out port. Injected packets
  * have lower priority than packets in the ring.
  */

/*
 *       /|--------------buffer-out1
 *  in1 -||
 *       \|---+
 *            |
 *       /|---|-------|\
 *  in0 -||   |       ||-buffer-out0
 *       \|-+ |     +-|/
 *          | |     |
 *          ---     |
 *          \_/     |
 *           |      |
 *         buffer   |
 *           |      |
 *          out    in
 */
class RingRouter (id: UInt, data_width: Int = 16, dest_width: Int = 10,
  buffer_depth: Int = 4)
    extends Module {
  val flit_width = data_width + 2

  val io = new Bundle {
    val ring_in0 = new NetworkLink(data_width).flip()
    val ring_in1 = new NetworkLink(data_width).flip()
    val ring_out0 = new NetworkLink(data_width)
    val ring_out1 = new NetworkLink(data_width)
    val in = new NetworkLink(data_width).flip()
    val out = new NetworkLink(data_width)
  }

  val buffer_ring0 = Module(new Queue(Bits(width = flit_width), buffer_depth))
  val buffer_ring1 = Module(new Queue(Bits(width = flit_width), buffer_depth))
  val buffer_local = Module(new Queue(Bits(width = flit_width), buffer_depth))

  val in0_demux = Module(new RingDemux(data_width, id, dest_width))
  val in1_demux = Module(new RingDemux(data_width, id, dest_width))

  val local_mux = Module(new RingMuxRR(data_width))
  val out0_mux = Module(new RingMux(data_width))

  // Connect outputs to buffers
  io.ring_out0 <> buffer_ring0.io.deq
  io.ring_out1 <> buffer_ring1.io.deq
  io.out <> buffer_local.io.deq

  // Connect inputs to demuxes
  in0_demux.io.in <> io.ring_in0
  in1_demux.io.in <> io.ring_in1

  // Connect input 1 path to output buffer (bypass)
  buffer_ring1.io.enq <> in1_demux.io.out1

  // Connect inputs for local to RR mux and mux to buffer
  local_mux.io.in0 <> in0_demux.io.out0
  local_mux.io.in1 <> in1_demux.io.out0
  buffer_local.io.enq <> local_mux.io.out

  // Connect ring mux to bypass in ring 0 and local and to buffer
  out0_mux.io.in0 <> in0_demux.io.out1
  out0_mux.io.in1 <> io.in
  buffer_ring0.io.enq <> out0_mux.io.out
}

class RingRouterTests(c: RingRouter) extends Tester(c) {
  /* STEP 0 */
  // Check forwarding in ring 1
  poke(c.io.ring_in1.valid, 1)
  poke(c.io.ring_in1.bits, 0x2abcd)
  poke(c.io.ring_out1.ready, 1)
  expect(c.io.ring_in1.ready, 1)
  expect(c.io.ring_in0.ready, 0)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 1 */
  // tail transfer
  // head should come out now
  poke(c.io.ring_in1.valid, 1)
  poke(c.io.ring_in1.bits, 0x1ef00)
  expect(c.io.ring_in1.ready, 1)
  expect(c.io.ring_in0.ready, 0)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 1)
  expect(c.io.ring_out1.bits, 0x2abcd)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 2 */
  // Check buffering (assumes depth 3)
  // Let the old tail flit pass
  poke(c.io.ring_in1.valid, 1)
  poke(c.io.ring_in1.bits, 0x2ffff)
  expect(c.io.ring_in1.ready, 1)
  expect(c.io.ring_in0.ready, 0)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 1)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 3 */
  poke(c.io.ring_in1.valid, 1)
  poke(c.io.ring_in1.bits, 0x01111)
  poke(c.io.ring_out1.ready, 0)
  expect(c.io.ring_in1.ready, 1)
  expect(c.io.ring_in0.ready, 0)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 1)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 4 */
  poke(c.io.ring_in1.valid, 1)
  poke(c.io.ring_in1.bits, 0x02222)
  expect(c.io.ring_in1.ready, 1)
  expect(c.io.ring_in0.ready, 0)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 1)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 5 */
  // Backpressure on ring 1
  poke(c.io.ring_in1.valid, 1)
  poke(c.io.ring_in1.bits, 0x13333)
  poke(c.io.ring_out1.ready, 1)
  expect(c.io.ring_in1.ready, 0)
  expect(c.io.ring_in0.ready, 0)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 1)
  expect(c.io.ring_out1.bits, 0x2ffff)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 6 */
  // Drain ring 1 from here on
  // Start test for ring 0
  // Start transfer on ring for forward
  poke(c.io.ring_out1.ready, 1)
  poke(c.io.ring_in0.valid, 1)
  poke(c.io.ring_in0.bits, 0x2dead)
  poke(c.io.ring_out0.ready, 1)
  expect(c.io.ring_in1.ready, 1)
  expect(c.io.ring_in0.ready, 1)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 1)
  expect(c.io.ring_out1.bits, 0x01111)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 7 */
  // (still drain ring 1)
  // Continue and complete worm on ring 0
  // Start worm from local
  // Expect header of ring 0 at output
  poke(c.io.ring_in1.valid, 0)
  poke(c.io.ring_in0.bits, 0x1beef)
  poke(c.io.in.valid, 1)
  poke(c.io.in.bits, 0x20123)
  expect(c.io.ring_in0.ready, 1)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 1)
  expect(c.io.ring_out1.bits, 0x02222)
  expect(c.io.ring_out0.valid, 1)
  expect(c.io.ring_out0.bits, 0x2dead)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 8 */
  // (complete drain ring 1)
  // Start new worm on ring 0 for forward
  // Start worm from local
  // Ring 0 has precedence
  // Expect tail of ring 0 at output
  poke(c.io.ring_in0.bits, 0x2abcd)
  expect(c.io.ring_in0.ready, 1)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 1)
  expect(c.io.ring_out1.bits, 0x13333)
  expect(c.io.ring_out0.valid, 1)
  expect(c.io.ring_out0.bits, 0x1beef)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 9 */
  // Finish second worm on ring 0 for forward
  // Start worm from local
  // Ring 0 has precedence
  // Expect second header of ring 0 at output
  poke(c.io.ring_in0.bits, 0x1abcd)
  expect(c.io.ring_in0.ready, 1)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 1)
  expect(c.io.ring_out0.bits, 0x2abcd)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 10 */
  // Worm from local gets forwarded now
  // Expect second tail of ring 0 at output
  poke(c.io.ring_in0.valid, 0)
  expect(c.io.ring_in0.ready, 0)
  expect(c.io.in.ready, 1)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 1)
  expect(c.io.ring_out0.bits, 0x1abcd)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 11 */
  // Continue local worm
  // Start another ring worm, this must not be served
  // Expect head of local at output
  poke(c.io.in.bits, 0x14567)
  poke(c.io.ring_in0.bits, 0x3ffff)
  poke(c.io.ring_in0.valid, 1)
  expect(c.io.ring_in0.ready, 0)
  expect(c.io.in.ready, 1)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 1)
  expect(c.io.ring_out0.bits, 0x20123)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 12 */
  // Finish test on ring 0
  // The ring 0 forward gets served
  // Expect tail of local at output
  poke(c.io.in.valid, 0)
  expect(c.io.ring_in0.ready, 1)
  expect(c.io.in.ready, 0)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 1)
  expect(c.io.ring_out0.bits, 0x14567)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 13 */
  // Expect single flit of ring at output
  poke(c.io.in.valid, 0)
  poke(c.io.ring_in0.valid, 0)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 1)
  expect(c.io.ring_out0.bits, 0x3ffff)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 14 */
  // Test local output
  poke(c.io.ring_in0.valid, 1)
  poke(c.io.ring_in0.bits, 0x20000)
  poke(c.io.ring_in1.valid, 1)
  poke(c.io.ring_in1.bits, 0x20001)
  poke(c.io.out.ready, 1)
  expect(c.io.ring_in0.ready, 1)
  expect(c.io.ring_in1.ready, 0)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 0)
  step(1)
  /* STEP 15 */
  poke(c.io.ring_in0.valid, 1)
  poke(c.io.ring_in0.bits, 0x3fff0)
  poke(c.io.ring_in1.valid, 1)
  poke(c.io.ring_in1.bits, 0x20001)
  expect(c.io.ring_in0.ready, 1)
  expect(c.io.ring_in1.ready, 0)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x20000)
  step(1)
  /* STEP 16 */
  poke(c.io.ring_in0.valid, 0)
  expect(c.io.ring_in0.ready, 0)
  expect(c.io.ring_in1.ready, 1)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x3fff0)
  step(1)
  /* STEP 17 */
  poke(c.io.ring_in1.bits, 0x3fff1)
  expect(c.io.ring_in0.ready, 0)
  expect(c.io.ring_in1.ready, 1)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x20001)
  step(1)
  /* STEP 18 */
  poke(c.io.ring_in1.valid, 0)
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 1)
  expect(c.io.out.bits, 0x3fff1)
  step(1)
  /* STEP 19 */
  expect(c.io.ring_out1.valid, 0)
  expect(c.io.ring_out0.valid, 0)
  expect(c.io.out.valid, 0)
  step(1)

}
