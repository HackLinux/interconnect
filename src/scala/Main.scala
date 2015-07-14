package OpenSoCDebug

import Chisel._

object Interconnect {
  def main(args: Array[String]): Unit = {
    val chiselargs = args.slice(1, args.length)
    args(0) match {
      case "RingDemuxTests" =>
        chiselMainTest(chiselargs,
          () => Module(new RingDemux(16, UInt(0), 10))) {
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
          () => Module(new RingRouter(16, UInt(0), 10))) {
          c => new RingRouterTests(c)
        }
      case "RingRouter" =>
        chiselMain(chiselargs,
          () => Module(new RingRouter(data_width = 16,
            id = UInt(0), dest_width = 10)))
    }
  }
}
