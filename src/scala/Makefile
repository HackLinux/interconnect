VERILOGDIR=../verilog/generated

all: $(VERILOGDIR)/RingRouter.v

RingDemuxTests:
	sbt "run RingDemuxTests --genHarness --compile --test --vcd --debug"

RingMuxTests:
	sbt "run RingMuxTests --genHarness --compile --test --vcd --debug"

RingMuxRRTests:
	sbt "run RingMuxRRTests --genHarness --compile --test --vcd --debug"

RingRouterTests:
	sbt "run RingRouterTests --genHarness --compile --test --vcd --debug"

$(VERILOGDIR)/RingRouter.v: compile
	sbt "run RingRouter --backend v"
	mv RingRouter.v $(VERILOGDIR)/RingRouter.v

compile:
	sbt compile
