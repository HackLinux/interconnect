VERILOGDIR=../verilog/generated

all: $(VERILOGDIR)/RingRouter.v $(VERILOGDIR)/Ring4.v

RingDemuxTests:
	sbt "run RingDemuxTests --genHarness --compile --test --vcd --debug"

RingMuxTests:
	sbt "run RingMuxTests --genHarness --compile --test --vcd --debug"

RingMuxRRTests:
	sbt "run RingMuxRRTests --genHarness --compile --test --vcd --debug"

RingRouterTests:
	sbt "run RingRouterTests --genHarness --compile --test --vcd --debug"

RingTests:
	sbt "run RingTests --genHarness --compile --test --vcd --debug"

$(VERILOGDIR)/RingRouter.v: compile
	sbt "run RingRouter --backend v"
	mv RingRouter.v $(VERILOGDIR)/RingRouter.v

$(VERILOGDIR)/Ring4.v: compile
	sbt "run Ring4 --backend v"
	mv Ring.v $(VERILOGDIR)/Ring4.v

compile:
	sbt compile
