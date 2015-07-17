module Queue(input clk, input reset,
    output io_enq_ready,
    input  io_enq_valid,
    input [17:0] io_enq_bits,
    input  io_deq_ready,
    output io_deq_valid,
    output[17:0] io_deq_bits,
    output[2:0] io_count
);

  wire[2:0] T0;
  wire[1:0] ptr_diff;
  reg [1:0] R1;
  wire[1:0] T15;
  wire[1:0] T2;
  wire[1:0] T3;
  wire do_deq;
  reg [1:0] R4;
  wire[1:0] T16;
  wire[1:0] T5;
  wire[1:0] T6;
  wire do_enq;
  wire T7;
  wire ptr_match;
  reg  maybe_full;
  wire T17;
  wire T8;
  wire T9;
  wire[17:0] T10;
  reg [17:0] ram [3:0];
  wire[17:0] T11;
  wire T12;
  wire empty;
  wire T13;
  wire T14;
  wire full;

`ifndef SYNTHESIS
// synthesis translate_off
  integer initvar;
  initial begin
    #0.002;
    R1 = {1{$random}};
    R4 = {1{$random}};
    maybe_full = {1{$random}};
    for (initvar = 0; initvar < 4; initvar = initvar+1)
      ram[initvar] = {1{$random}};
  end
// synthesis translate_on
`endif

  assign io_count = T0;
  assign T0 = {T7, ptr_diff};
  assign ptr_diff = R4 - R1;
  assign T15 = reset ? 2'h0 : T2;
  assign T2 = do_deq ? T3 : R1;
  assign T3 = R1 + 2'h1;
  assign do_deq = io_deq_ready & io_deq_valid;
  assign T16 = reset ? 2'h0 : T5;
  assign T5 = do_enq ? T6 : R4;
  assign T6 = R4 + 2'h1;
  assign do_enq = io_enq_ready & io_enq_valid;
  assign T7 = maybe_full & ptr_match;
  assign ptr_match = R4 == R1;
  assign T17 = reset ? 1'h0 : T8;
  assign T8 = T9 ? do_enq : maybe_full;
  assign T9 = do_enq != do_deq;
  assign io_deq_bits = T10;
  assign T10 = ram[R1];
  assign io_deq_valid = T12;
  assign T12 = empty ^ 1'h1;
  assign empty = ptr_match & T13;
  assign T13 = maybe_full ^ 1'h1;
  assign io_enq_ready = T14;
  assign T14 = full ^ 1'h1;
  assign full = ptr_match & maybe_full;

  always @(posedge clk) begin
    if(reset) begin
      R1 <= 2'h0;
    end else if(do_deq) begin
      R1 <= T3;
    end
    if(reset) begin
      R4 <= 2'h0;
    end else if(do_enq) begin
      R4 <= T6;
    end
    if(reset) begin
      maybe_full <= 1'h0;
    end else if(T9) begin
      maybe_full <= do_enq;
    end
    if (do_enq)
      ram[R4] <= io_enq_bits;
  end
endmodule

module RingDemux(input clk, input reset,
    input [9:0] io_id,
    output io_in_ready,
    input  io_in_valid,
    input [17:0] io_in_bits,
    input  io_out0_ready,
    output io_out0_valid,
    output[17:0] io_out0_bits,
    input  io_out1_ready,
    output io_out1_valid,
    output[17:0] io_out1_bits
);

  wire T0;
  wire T1;
  wire T2;
  wire T3;
  wire in_out1;
  wire in_out0;
  wire[9:0] in_dest;
  wire T4;
  wire T5;
  reg [1:0] in_worm;
  wire[1:0] T37;
  wire[1:0] nxt_in_worm;
  wire[1:0] T6;
  wire[1:0] T7;
  wire[1:0] T8;
  wire[1:0] T9;
  wire T10;
  wire T11;
  wire T12;
  wire T13;
  wire in_is_last;
  wire T14;
  wire[1:0] in_type;
  wire T15;
  wire T16;
  wire T17;
  wire T18;
  wire T19;
  wire T20;
  wire T21;
  wire T22;
  wire T23;
  wire T24;
  wire T25;
  wire T26;
  wire T27;
  wire T28;
  wire T29;
  wire T30;
  wire T31;
  wire T32;
  wire T33;
  wire T34;
  wire T35;
  wire T36;

`ifndef SYNTHESIS
// synthesis translate_off
  integer initvar;
  initial begin
    #0.002;
    in_worm = {1{$random}};
  end
// synthesis translate_on
`endif

  assign io_out1_bits = io_in_bits;
  assign io_out1_valid = T0;
  assign T0 = T26 ? io_in_valid : T1;
  assign T1 = T4 ? T2 : 1'h0;
  assign T2 = T3;
  assign T3 = in_out1;
  assign in_out1 = in_out0 ^ 1'h1;
  assign in_out0 = in_dest == io_id;
  assign in_dest = io_in_bits[4'hf:3'h6];
  assign T4 = T5 & io_in_valid;
  assign T5 = 2'h0 == in_worm;
  assign T37 = reset ? 2'h0 : nxt_in_worm;
  assign nxt_in_worm = T6;
  assign T6 = T23 ? 2'h0 : T7;
  assign T7 = T19 ? 2'h0 : T8;
  assign T8 = T15 ? 2'h2 : T9;
  assign T9 = T10 ? 2'h1 : in_worm;
  assign T10 = T12 & T11;
  assign T11 = in_out0 & io_out0_ready;
  assign T12 = T4 & T13;
  assign T13 = in_is_last ^ 1'h1;
  assign in_is_last = T14 == 1'h1;
  assign T14 = in_type[1'h0:1'h0];
  assign in_type = io_in_bits[5'h11:5'h10];
  assign T15 = T12 & T16;
  assign T16 = T18 & T17;
  assign T17 = in_out1 & io_out1_ready;
  assign T18 = T11 ^ 1'h1;
  assign T19 = T22 & T20;
  assign T20 = T21 & io_in_valid;
  assign T21 = in_is_last & io_out0_ready;
  assign T22 = 2'h1 == in_worm;
  assign T23 = T26 & T24;
  assign T24 = T25 & io_in_valid;
  assign T25 = in_is_last & io_out1_ready;
  assign T26 = 2'h2 == in_worm;
  assign io_out0_bits = io_in_bits;
  assign io_out0_valid = T27;
  assign T27 = T22 ? io_in_valid : T28;
  assign T28 = T4 ? T29 : 1'h0;
  assign T29 = T30;
  assign T30 = in_out0;
  assign io_in_ready = T31;
  assign T31 = T26 ? io_out1_ready : T32;
  assign T32 = T22 ? io_out0_ready : T33;
  assign T33 = T5 ? T34 : 1'h0;
  assign T34 = T36 | T35;
  assign T35 = in_out1 & io_out1_ready;
  assign T36 = in_out0 & io_out0_ready;

  always @(posedge clk) begin
    if(reset) begin
      in_worm <= 2'h0;
    end else begin
      in_worm <= nxt_in_worm;
    end
  end
endmodule

module RingMuxRR(input clk, input reset,
    output io_in0_ready,
    input  io_in0_valid,
    input [17:0] io_in0_bits,
    output io_in1_ready,
    input  io_in1_valid,
    input [17:0] io_in1_bits,
    input  io_out_ready,
    output io_out_valid,
    output[17:0] io_out_bits
);

  wire[17:0] T0;
  wire[17:0] T1;
  wire[17:0] T2;
  wire[17:0] T3;
  wire[17:0] T4;
  wire[17:0] T5;
  wire T6;
  wire T7;
  wire in0_is_first;
  wire T8;
  wire[1:0] in0_type;
  wire T9;
  reg [1:0] active;
  wire[1:0] T66;
  wire[1:0] nxt_active;
  wire[1:0] T10;
  wire[1:0] T11;
  wire[1:0] T12;
  wire[1:0] T13;
  wire[1:0] T14;
  wire[1:0] T15;
  wire[1:0] T16;
  wire[1:0] T17;
  wire T18;
  wire T19;
  wire in0_is_last;
  wire T20;
  wire T21;
  wire T22;
  wire T23;
  wire T24;
  wire T25;
  wire in1_is_last;
  wire T26;
  wire[1:0] in1_type;
  wire T27;
  wire T28;
  wire T29;
  wire T30;
  wire T31;
  wire T32;
  wire T33;
  wire T34;
  wire T35;
  wire T36;
  wire T37;
  wire T38;
  wire T39;
  wire T40;
  wire T41;
  wire T42;
  wire in1_is_first;
  wire T43;
  wire T44;
  wire T45;
  wire T46;
  wire T47;
  wire T48;
  wire T49;
  wire T50;
  wire T51;
  wire T52;
  wire T53;
  wire T54;
  wire T55;
  wire T56;
  wire T57;
  wire T58;
  wire T59;
  wire T60;
  wire T61;
  wire T62;
  wire T63;
  wire T64;
  wire T65;

`ifndef SYNTHESIS
// synthesis translate_off
  integer initvar;
  initial begin
    #0.002;
    active = {1{$random}};
  end
// synthesis translate_on
`endif

  assign io_out_bits = T0;
  assign T0 = T53 ? io_in1_bits : T1;
  assign T1 = T52 ? io_in0_bits : T2;
  assign T2 = T48 ? io_in0_bits : T3;
  assign T3 = T45 ? io_in1_bits : T4;
  assign T4 = T40 ? io_in1_bits : T5;
  assign T5 = T6 ? io_in0_bits : 18'h0;
  assign T6 = T9 & T7;
  assign T7 = io_in0_valid & in0_is_first;
  assign in0_is_first = T8 == 1'h1;
  assign T8 = in0_type[1'h1:1'h1];
  assign in0_type = io_in0_bits[5'h11:5'h10];
  assign T9 = 2'h0 == active;
  assign T66 = reset ? 2'h0 : nxt_active;
  assign nxt_active = T10;
  assign T10 = T37 ? 2'h0 : T11;
  assign T11 = T34 ? 2'h1 : T12;
  assign T12 = T32 ? 2'h3 : T13;
  assign T13 = T30 ? 2'h0 : T14;
  assign T14 = T27 ? 2'h3 : T15;
  assign T15 = T24 ? 2'h3 : T16;
  assign T16 = T22 ? 2'h1 : T17;
  assign T17 = T18 ? 2'h2 : active;
  assign T18 = T21 & T19;
  assign T19 = in0_is_last ^ 1'h1;
  assign in0_is_last = T20 == 1'h1;
  assign T20 = in0_type[1'h0:1'h0];
  assign T21 = T6 & io_out_ready;
  assign T22 = T21 & T23;
  assign T23 = T19 ^ 1'h1;
  assign T24 = T40 & T25;
  assign T25 = in1_is_last ^ 1'h1;
  assign in1_is_last = T26 == 1'h1;
  assign T26 = in1_type[1'h0:1'h0];
  assign in1_type = io_in1_bits[5'h11:5'h10];
  assign T27 = T29 & T28;
  assign T28 = in1_is_last ^ 1'h1;
  assign T29 = T45 & io_out_ready;
  assign T30 = T29 & T31;
  assign T31 = T28 ^ 1'h1;
  assign T32 = T48 & T33;
  assign T33 = in0_is_last ^ 1'h1;
  assign T34 = T52 & T35;
  assign T35 = T36 & in0_is_last;
  assign T36 = io_in0_valid & io_out_ready;
  assign T37 = T53 & T38;
  assign T38 = T39 & in1_is_last;
  assign T39 = io_in1_valid & io_out_ready;
  assign T40 = T9 & T41;
  assign T41 = T44 & T42;
  assign T42 = io_in1_valid & in1_is_first;
  assign in1_is_first = T43 == 1'h1;
  assign T43 = in1_type[1'h1:1'h1];
  assign T44 = T7 ^ 1'h1;
  assign T45 = T47 & T46;
  assign T46 = io_in1_valid & in1_is_first;
  assign T47 = 2'h1 == active;
  assign T48 = T47 & T49;
  assign T49 = T51 & T50;
  assign T50 = io_in0_valid & in0_is_first;
  assign T51 = T46 ^ 1'h1;
  assign T52 = 2'h2 == active;
  assign T53 = 2'h3 == active;
  assign io_out_valid = T54;
  assign T54 = T53 ? io_in1_valid : T55;
  assign T55 = T52 ? io_in0_valid : T56;
  assign T56 = T48 ? io_in0_valid : T57;
  assign T57 = T45 ? io_in1_valid : T58;
  assign T58 = T40 ? io_in1_valid : T59;
  assign T59 = T6 ? io_in0_valid : 1'h0;
  assign io_in1_ready = T60;
  assign T60 = T53 ? io_out_ready : T61;
  assign T61 = T45 ? io_out_ready : T62;
  assign T62 = T40 ? io_out_ready : 1'h0;
  assign io_in0_ready = T63;
  assign T63 = T52 ? io_out_ready : T64;
  assign T64 = T48 ? io_out_ready : T65;
  assign T65 = T6 ? io_out_ready : 1'h0;

  always @(posedge clk) begin
    if(reset) begin
      active <= 2'h0;
    end else begin
      active <= nxt_active;
    end
  end
endmodule

module RingMux(input clk, input reset,
    output io_in0_ready,
    input  io_in0_valid,
    input [17:0] io_in0_bits,
    output io_in1_ready,
    input  io_in1_valid,
    input [17:0] io_in1_bits,
    input  io_out_ready,
    output io_out_valid,
    output[17:0] io_out_bits
);

  wire[17:0] T0;
  wire[17:0] T1;
  wire[17:0] T2;
  wire[17:0] T3;
  wire T4;
  wire T5;
  wire in0_is_first;
  wire T6;
  wire[1:0] in0_type;
  wire T7;
  reg [1:0] active;
  wire[1:0] T40;
  wire[1:0] nxt_active;
  wire[1:0] T8;
  wire[1:0] T9;
  wire[1:0] T10;
  wire[1:0] T11;
  wire T12;
  wire T13;
  wire in0_is_last;
  wire T14;
  wire T15;
  wire T16;
  wire T17;
  wire in1_is_last;
  wire T18;
  wire[1:0] in1_type;
  wire T19;
  wire T20;
  wire T21;
  wire T22;
  wire T23;
  wire T24;
  wire T25;
  wire T26;
  wire T27;
  wire in1_is_first;
  wire T28;
  wire T29;
  wire T30;
  wire T31;
  wire T32;
  wire T33;
  wire T34;
  wire T35;
  wire T36;
  wire T37;
  wire T38;
  wire T39;

`ifndef SYNTHESIS
// synthesis translate_off
  integer initvar;
  initial begin
    #0.002;
    active = {1{$random}};
  end
// synthesis translate_on
`endif

  assign io_out_bits = T0;
  assign T0 = T31 ? io_in1_bits : T1;
  assign T1 = T30 ? io_in0_bits : T2;
  assign T2 = T25 ? io_in1_bits : T3;
  assign T3 = T4 ? io_in0_bits : 18'h0;
  assign T4 = T7 & T5;
  assign T5 = io_in0_valid & in0_is_first;
  assign in0_is_first = T6 == 1'h1;
  assign T6 = in0_type[1'h1:1'h1];
  assign in0_type = io_in0_bits[5'h11:5'h10];
  assign T7 = 2'h0 == active;
  assign T40 = reset ? 2'h0 : nxt_active;
  assign nxt_active = T8;
  assign T8 = T22 ? 2'h0 : T9;
  assign T9 = T19 ? 2'h0 : T10;
  assign T10 = T15 ? 2'h2 : T11;
  assign T11 = T12 ? 2'h1 : active;
  assign T12 = T4 & T13;
  assign T13 = in0_is_last ^ 1'h1;
  assign in0_is_last = T14 == 1'h1;
  assign T14 = in0_type[1'h0:1'h0];
  assign T15 = T25 & T16;
  assign T16 = T17 & io_out_ready;
  assign T17 = in1_is_last ^ 1'h1;
  assign in1_is_last = T18 == 1'h1;
  assign T18 = in1_type[1'h0:1'h0];
  assign in1_type = io_in1_bits[5'h11:5'h10];
  assign T19 = T30 & T20;
  assign T20 = T21 & in0_is_last;
  assign T21 = io_in0_valid & io_out_ready;
  assign T22 = T31 & T23;
  assign T23 = T24 & in1_is_last;
  assign T24 = io_in1_valid & io_out_ready;
  assign T25 = T7 & T26;
  assign T26 = T29 & T27;
  assign T27 = io_in1_valid & in1_is_first;
  assign in1_is_first = T28 == 1'h1;
  assign T28 = in1_type[1'h1:1'h1];
  assign T29 = T5 ^ 1'h1;
  assign T30 = 2'h1 == active;
  assign T31 = 2'h2 == active;
  assign io_out_valid = T32;
  assign T32 = T31 ? io_in1_valid : T33;
  assign T33 = T30 ? io_in0_valid : T34;
  assign T34 = T25 ? io_in1_valid : T35;
  assign T35 = T4 ? io_in0_valid : 1'h0;
  assign io_in1_ready = T36;
  assign T36 = T31 ? io_out_ready : T37;
  assign T37 = T25 ? io_out_ready : 1'h0;
  assign io_in0_ready = T38;
  assign T38 = T30 ? io_out_ready : T39;
  assign T39 = T4 ? io_out_ready : 1'h0;

  always @(posedge clk) begin
    if(reset) begin
      active <= 2'h0;
    end else begin
      active <= nxt_active;
    end
  end
endmodule

module RingRouter(input clk, input reset,
    input [9:0] io_id,
    output io_ring_in0_ready,
    input  io_ring_in0_valid,
    input [17:0] io_ring_in0_bits,
    output io_ring_in1_ready,
    input  io_ring_in1_valid,
    input [17:0] io_ring_in1_bits,
    input  io_ring_out0_ready,
    output io_ring_out0_valid,
    output[17:0] io_ring_out0_bits,
    input  io_ring_out1_ready,
    output io_ring_out1_valid,
    output[17:0] io_ring_out1_bits,
    output io_in_ready,
    input  io_in_valid,
    input [17:0] io_in_bits,
    input  io_out_ready,
    output io_out_valid,
    output[17:0] io_out_bits
);

  wire buffer_ring0_io_enq_ready;
  wire buffer_ring0_io_deq_valid;
  wire[17:0] buffer_ring0_io_deq_bits;
  wire buffer_ring1_io_enq_ready;
  wire buffer_ring1_io_deq_valid;
  wire[17:0] buffer_ring1_io_deq_bits;
  wire buffer_local_io_enq_ready;
  wire buffer_local_io_deq_valid;
  wire[17:0] buffer_local_io_deq_bits;
  wire in0_demux_io_in_ready;
  wire in0_demux_io_out0_valid;
  wire[17:0] in0_demux_io_out0_bits;
  wire in0_demux_io_out1_valid;
  wire[17:0] in0_demux_io_out1_bits;
  wire in1_demux_io_in_ready;
  wire in1_demux_io_out0_valid;
  wire[17:0] in1_demux_io_out0_bits;
  wire in1_demux_io_out1_valid;
  wire[17:0] in1_demux_io_out1_bits;
  wire local_mux_io_in0_ready;
  wire local_mux_io_in1_ready;
  wire local_mux_io_out_valid;
  wire[17:0] local_mux_io_out_bits;
  wire out0_mux_io_in0_ready;
  wire out0_mux_io_in1_ready;
  wire out0_mux_io_out_valid;
  wire[17:0] out0_mux_io_out_bits;


  assign io_out_bits = buffer_local_io_deq_bits;
  assign io_out_valid = buffer_local_io_deq_valid;
  assign io_in_ready = out0_mux_io_in1_ready;
  assign io_ring_out1_bits = buffer_ring1_io_deq_bits;
  assign io_ring_out1_valid = buffer_ring1_io_deq_valid;
  assign io_ring_out0_bits = buffer_ring0_io_deq_bits;
  assign io_ring_out0_valid = buffer_ring0_io_deq_valid;
  assign io_ring_in1_ready = in1_demux_io_in_ready;
  assign io_ring_in0_ready = in0_demux_io_in_ready;
  Queue buffer_ring0(.clk(clk), .reset(reset),
       .io_enq_ready( buffer_ring0_io_enq_ready ),
       .io_enq_valid( out0_mux_io_out_valid ),
       .io_enq_bits( out0_mux_io_out_bits ),
       .io_deq_ready( io_ring_out0_ready ),
       .io_deq_valid( buffer_ring0_io_deq_valid ),
       .io_deq_bits( buffer_ring0_io_deq_bits )
       //.io_count(  )
  );
  Queue buffer_ring1(.clk(clk), .reset(reset),
       .io_enq_ready( buffer_ring1_io_enq_ready ),
       .io_enq_valid( in1_demux_io_out1_valid ),
       .io_enq_bits( in1_demux_io_out1_bits ),
       .io_deq_ready( io_ring_out1_ready ),
       .io_deq_valid( buffer_ring1_io_deq_valid ),
       .io_deq_bits( buffer_ring1_io_deq_bits )
       //.io_count(  )
  );
  Queue buffer_local(.clk(clk), .reset(reset),
       .io_enq_ready( buffer_local_io_enq_ready ),
       .io_enq_valid( local_mux_io_out_valid ),
       .io_enq_bits( local_mux_io_out_bits ),
       .io_deq_ready( io_out_ready ),
       .io_deq_valid( buffer_local_io_deq_valid ),
       .io_deq_bits( buffer_local_io_deq_bits )
       //.io_count(  )
  );
  RingDemux in0_demux(.clk(clk), .reset(reset),
       .io_id( io_id ),
       .io_in_ready( in0_demux_io_in_ready ),
       .io_in_valid( io_ring_in0_valid ),
       .io_in_bits( io_ring_in0_bits ),
       .io_out0_ready( local_mux_io_in0_ready ),
       .io_out0_valid( in0_demux_io_out0_valid ),
       .io_out0_bits( in0_demux_io_out0_bits ),
       .io_out1_ready( out0_mux_io_in0_ready ),
       .io_out1_valid( in0_demux_io_out1_valid ),
       .io_out1_bits( in0_demux_io_out1_bits )
  );
  RingDemux in1_demux(.clk(clk), .reset(reset),
       .io_id( io_id ),
       .io_in_ready( in1_demux_io_in_ready ),
       .io_in_valid( io_ring_in1_valid ),
       .io_in_bits( io_ring_in1_bits ),
       .io_out0_ready( local_mux_io_in1_ready ),
       .io_out0_valid( in1_demux_io_out0_valid ),
       .io_out0_bits( in1_demux_io_out0_bits ),
       .io_out1_ready( buffer_ring1_io_enq_ready ),
       .io_out1_valid( in1_demux_io_out1_valid ),
       .io_out1_bits( in1_demux_io_out1_bits )
  );
  RingMuxRR local_mux(.clk(clk), .reset(reset),
       .io_in0_ready( local_mux_io_in0_ready ),
       .io_in0_valid( in0_demux_io_out0_valid ),
       .io_in0_bits( in0_demux_io_out0_bits ),
       .io_in1_ready( local_mux_io_in1_ready ),
       .io_in1_valid( in1_demux_io_out0_valid ),
       .io_in1_bits( in1_demux_io_out0_bits ),
       .io_out_ready( buffer_local_io_enq_ready ),
       .io_out_valid( local_mux_io_out_valid ),
       .io_out_bits( local_mux_io_out_bits )
  );
  RingMux out0_mux(.clk(clk), .reset(reset),
       .io_in0_ready( out0_mux_io_in0_ready ),
       .io_in0_valid( in0_demux_io_out1_valid ),
       .io_in0_bits( in0_demux_io_out1_bits ),
       .io_in1_ready( out0_mux_io_in1_ready ),
       .io_in1_valid( io_in_valid ),
       .io_in1_bits( io_in_bits ),
       .io_out_ready( buffer_ring0_io_enq_ready ),
       .io_out_valid( out0_mux_io_out_valid ),
       .io_out_bits( out0_mux_io_out_bits )
  );
endmodule

