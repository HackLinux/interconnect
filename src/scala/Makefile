VERILOGDIR=../verilog

all: RingRouter.v

RingDemuxTests:
	sbt "run RingDemuxTests --genHarness --compile --test --vcd"

RingMuxTests:
	sbt "run RingMuxTests --genHarness --compile --test --vcd"

RingMuxRRTests:
	sbt "run RingMuxRRTests --genHarness --compile --test --vcd"

RingRouterTests:
	sbt "run RingRouterTests --genHarness --compile --test --vcd"

RingRouter.v: compile
	sbt "run RingRouter --backend v"

compile:
	sbt compile
