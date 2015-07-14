package OpenSoCDebug

import Chisel._

class NetworkLink (data_width: Int)
    extends DecoupledIO(Bits(width=data_width+2)) {

}

/** Ring Router
  * 
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
class RingRouter (data_width: Int, id: UInt, dest_width: Int)
    extends Module {
  val flit_width = data_width + 2
  val buffer_depth = 4;

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
  // First check 
}
