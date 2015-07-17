package OpenSoCDebug

import Chisel._

object Interconnect {
  def main(args: Array[String]): Unit = {
    val chiselargs = args.slice(1, args.length)
    args(0) match {
      case "RingDemuxTests" =>
        chiselMainTest(chiselargs,
          () => Module(new RingDemux(16, 10))) {
          c => new RingDemuxTests(c)
        }
      case "RingMuxTests" =>
        chiselMainTest(chiselargs,
          () => Module(new RingMux(16))) {
          c => new RingMuxTests(c)
        }
      case "RingMuxRRTests" =>
        chiselMainTest(chiselargs,
          () => Module(new RingMuxRR(16))) {
          c => new RingMuxRRTests(c)
        }
      case "RingRouterTests" =>
        chiselMainTest(chiselargs,
          () => Module(new RingRouter(data_width = 16, dest_width = 10,
            buffer_depth = 3))) {
          c => new RingRouterTests(c)
        }
      case "RingTests" =>
        chiselMainTest(chiselargs,
          () => Module(new Ring(num_ports = 4))) {
          c => new RingTests(c)
        }
      case "RingRouter" =>
        chiselMain(chiselargs,
          () => Module(new RingRouter(data_width = 16,
            dest_width = 10, buffer_depth = 4)))
      case "Ring4" =>
        chiselMain(chiselargs,
          () => Module(new Ring(data_width = 16,
            dest_width = 10, num_ports = 4)))
    }
  }
}
