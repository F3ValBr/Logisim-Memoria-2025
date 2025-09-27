package com.cburch.logisim.std.riscv;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.*;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.StringTokenizer;

abstract class Processor extends InstanceFactory {

    //processor attributes
    static final AttributeOption ENABLE
            = new AttributeOption("enable",Strings.getter("processorEnable"));
    static final AttributeOption DISABLE
            = new AttributeOption("disable",Strings.getter("processorDisable"));
    static final Attribute<AttributeOption> ENABLE_MEMORY
            = Attributes.forOption("setEnableMemory",Strings.getter("processorSetEnableMemory"),
            new AttributeOption[] { ENABLE,DISABLE});
    static final Attribute<Integer> FIRST_ADDR = Attributes.forHexInteger(
            "firstAddress",Strings.getter("processorFirstAddress"));
    static final Attribute<Integer> BOOT_ADDR = Attributes.forHexInteger(
            "bootAddress",Strings.getter("processorBootAddress"));
    static final Attribute<Integer> INTR_ADDR = Attributes.forHexInteger(
            "intrAddress",Strings.getter("processorIntrAddress"));
    static final Attribute<BitWidth> ADDR_ATTR = Attributes.forBitWidth(
            "addrWidth", Strings.getter("processorAddrWidthAttr"), 2, 24);
    static final Attribute<Integer> MULTIPLIER =Attributes.forIntegerMultiplierRange(
            "multiplier", Strings.getter("processorMultiplierAttr"), 0, 12);
    static Attribute<DataContents> CONTENTS_ATTR = new ContentsAttribute();

    //Port address
    static final int DATA = 0;
    static final int ADDR = 1;
    static final int BE = 2;
    static final int CLK = 3;
    static final int RD = 4;
    static final int WR = 5;
    static final int INTR = 6;
    static final int MEM_INPUTS = 7;
    static final BitWidth b32 = BitWidth.create(32);
    static final BitWidth b4 = BitWidth.create(4);
    static final int delay=10;
    Processor(String name,String keyName,String gif) {
        super(name, Strings.getter(keyName));
        setIconName(gif);
        setOffsetBounds(Bounds.create(-150, -150, 300, 300));
    }

    void configurePorts(Instance instance) {
        Port[] ps = new Port[MEM_INPUTS];
        ps[DATA] = new Port(150, 0, Port.INOUT, b32);
        ps[ADDR] = new Port(-150, 0, Port.OUTPUT, b32);
        ps[BE] = new Port(-150, 40, Port.OUTPUT, b4);
        ps[RD] = new Port(-150, 80, Port.OUTPUT, 1);
        ps[WR] = new Port(-150, 90, Port.OUTPUT, 1);
        ps[CLK] = new Port(0, 150, Port.INPUT, 1);
        ps[INTR] = new Port(150, 0, Port.INPUT, 1);
        configureTipPorts(ps);
        instance.setPorts(ps);
    }
    void configureTipPorts(Port[] ps) {
        ps[DATA].setToolTip(Strings.getter("processorDataTip"));
        ps[ADDR].setToolTip(Strings.getter("processorAddrTip"));
        ps[BE].setToolTip(Strings.getter("processorBeTip"));
        ps[RD].setToolTip(Strings.getter("processorRdTip"));
        ps[WR].setToolTip(Strings.getter("processorWrTip"));
        ps[CLK].setToolTip(Strings.getter("processorClkTip"));
        ps[INTR].setToolTip(Strings.getter("processorIntrTip"));
    }
    @Override
    protected void configureNewInstance(Instance instance) {
        configurePorts(instance);

    }
    @Override
    public AttributeSet createAttributeSet() {
        return new ProcessorAttributes();
    }
    HexFrame getHexFrame(Project proj, Instance instance) {
        return ProcessorAttributes.getHexFrame(instance.getAttributeValue(CONTENTS_ATTR), proj);
    }
    ProcessorData getData(Instance instance, CircuitState state){
        ProcessorData data =(ProcessorData) instance.getData(state);
        if(data == null){
            //,instance.getAttributeValue(INTR_ENABLE)
            data=new ProcessorData(instance.getAttributeValue(CONTENTS_ATTR),instance.getAttributeValue(BOOT_ADDR));
            instance.setData(state,data);
        }
        return data;
    }
    void clearContentAttributes(Instance instance) {
        instance.getAttributeValue(CONTENTS_ATTR).clear();
    }
    @Override
    public void propagate(InstanceState state) {
        ProcessorData data =(ProcessorData) state.getData();
        if(data == null) {//init data
            data=new ProcessorData(state.getAttributeValue(CONTENTS_ATTR),state.getAttributeValue(BOOT_ADDR));
            state.setData(data);
            state.setPort(WR,Value.FALSE,0);
            state.setPort(RD,Value.FALSE,0);
            state.setPort(ADDR,Value.createKnown(b32,0),0);
            state.setPort(BE,Value.createKnown(b4,0),0);
        }
        Value last=data.lastClock;
        data.lastClock=state.getPort(CLK);
        if(data.lastClock.equals(last)
                || !data.lastClock.isFullyDefined()
                || !last.isFullyDefined()) {return;}
        Value intr=state.getPort(INTR);
        //interruption handler
        if(intr==Value.TRUE && data.intr_enable && !(data.low & data.opcode!=0b0)){
            data.intrPC(state.getAttributeValue(INTR_ADDR));
            execute(state,data);
            if(data.low){               //SW extern, one cycle close ports and step PC
                propagateLow(state,data,last);
                data.stepPC(-4);
            }
            return;
        }
        if(data.low){
            propagateLow(state,data,last);
            return;
        }
        for(int i=0;i<(data.ls_PC ? 1 : state.getAttributeValue(MULTIPLIER));i++){
            //get instruction
            if( addressInProcessor(state,data.programCount>>2)) {
                int offset = state.getAttributeValue(FIRST_ADDR);
                int res = data.contents.get((data.programCount - offset) >>> 2);
                if (data.ls_PC) {
                    data.loadPC(res);
                    return;
                } else {
                    data.setInstruction(data.contents.get((data.programCount - offset) >>> 2));
                }
            }
            else{
                data.low=true;
                data.opcode=0;
                propagateLow(state,data,last);
                return;
            }
            execute(state,data);
        }
        if(data.low){
            propagateLow(state,data,last);
        }
    }
    public void propagateLow(InstanceState state, ProcessorData data,Value last) {
        //if(!(data.lastClock == Value.FALSE && last == Value.TRUE)){return;}
        boolean falling=(data.lastClock == Value.FALSE && last == Value.TRUE);
        boolean rising =(data.lastClock == Value.TRUE && last == Value.FALSE);
        if(falling){
            switch (data.opcode) {
                case 0b0:
                    goFetch(state, data);
                    break;
                case 0b10:
                    data.opcode=data.instruction.opcode;
                    if (data.opcode == 0b0000011) {
                        iTypeLoadLow(state,data);
                    }
                    if (data.opcode== 0b0100011) { //SB / SH / SW
                        sTypeLow(state,data);
                    }
                    break;
                //store data
                case 0b0100011:
                    state.setPort(WR,Value.FALSE,delay);
                    state.setPort(DATA,Value.createUnknown(b32),delay);
                    data.stepPC(4);
                    data.low=false;
                    break;
                //load data
                case  0b0000011:
                    Value res=state.getPort(DATA);
                    if(!res.isFullyDefined()) {
                        Value[] resList = res.getAll();
                        for (int i = 0; i < resList.length; i++) {
                            if(resList[i]==Value.ERROR) {return;} {}
                            if (resList[i] == Value.UNKNOWN) {
                                resList[i] = Value.FALSE;
                            }
                        }
                        res=Value.create(resList);
                    }
                    state.setPort(RD,Value.FALSE,delay);
                    data.stepPC(4);
                    data.low = false;
                    if(data.ls_PC){
                        data.loadPC(res.toIntValue());
                        data.intr_enable=true;
                        break;
                    }
                    int bs=state.getPort(ADDR).toIntValue()&0b11;
                    int v= selectByteData(data,bs,res.toIntValue());
                    data.register.setValue(data.instruction.d, v);
                    break;
            }
            return;
        }
        if(rising){
            if (data.opcode == 0b1) {
                fetch(state, data);
            }
        }
    }

    private void goFetch(InstanceState state, ProcessorData data){
        data.opcode=1;
        state.setPort(ADDR,Value.createKnown(b32,data.programCount),delay);
        state.setPort(RD,Value.TRUE,delay+1);
        state.setPort(BE,Value.createKnown(b4,15),delay);
    }
    private void fetch(InstanceState state, ProcessorData data){
        Value inst =state.getPort(DATA);
        if(inst.isFullyDefined()){
            data.low=false;
            state.setPort(RD,Value.FALSE,delay);
            if(data.ls_PC){data.loadPC(inst.toIntValue());return;}
            data.setInstruction(inst);
            execute(state,data);
        }
    }

    private void execute(InstanceState state, ProcessorData data){
        data.opcode=data.instruction.opcode;
        switch (data.opcode){
            case 0b0110011: // ADD / SUB / SLL / SLT / SLTU / XOR / SRL / SRA / OR / AND / MUL / DIV
                rType(data,data.getS1(),data.getS2());
                data.stepPC(4);
                break;
            case 0b1101111: //JAL
                data.register.setValue(data.instruction.d, data.programCount+4);
                data.stepPC(data.instruction.getImm());
                break;
            case 0b1100111: // JALR
                data.register.setValue(data.instruction.d, data.programCount+4);
                data.stepPCS1(data.instruction.getImm());
                break;
            case 0b0000011: // LB / LH / LW / LBU / LHU
                iTypeLoad(state,data);
                break;
            case 0b0010011: // ADDI / SLTI / SLTIU / XORI / ORI / ANDI / SLLI / SRLI / SRAI
                rType(data,data.getS1(),data.instruction.getImm());
                data.stepPC(4);
                break;
            case 0b0100011: //SB / SH / SW
                sType(state,data);
                break;
            case 0b1110011: // ECALL
                System.out.println("ECALL");
                iTypeEcall();
                data.stepPC(4);
                break;
            //B-type instructions
            case 0b1100011: // BEQ / BNE / BLT / BGE / BLTU / BGEU
                bType(data);
                break;

            //U-type instructions
            case 0b0110111: //LUI
                data.register.setValue(data.instruction.d,data.instruction.getImm());
                data.stepPC(4);
                break;
            case 0b0010111: //AUIPC
                data.register.setValue(data.instruction.d,data.programCount+data.instruction.getImm());
                data.stepPC(4);
                break;
            case 0b1000000: // enable interruption
                data.intr_enable=true;
                data.stepPC(4);
                break;
            case 0b1000001: // disable interruption
                data.intr_enable=false;
                data.stepPC(4);
                break;
            case 0b1000010: //MRET
                data.retPC();
                iTypeLoad(state,data);
                break;
            default:
                System.out.println(data.instruction.instruction);
                data.stepPC(4);
                break;
        }
    }

    private int selectByteData( ProcessorData data,int bs, int res){
        int func3=data.instruction.func3;
        switch (func3){
            case 0b000: // LB
                res = (res>>(bs<<3))&0xFF;
                if(((res&0x80))!=0){res|=0xFFFFFF00;}
                break;
            case 0b001: // LH
                res = (res>>(bs<<3))&0xFFFF;
                if(((res&0x8000))!=0){res|=0xFFFF0000;}
                break;
            case 0b100: // LBU
                res = (res>>(bs<<3))&0xFF;
                break;
            case 0b101: // LHU
                res = (res>>(bs<<3))&0xFFFF;
                break;
            default:
                break;
        }
        return res;
    }

    private void rType(ProcessorData data, int rs1, int rs2) {
        Instruction inst=data.instruction;
        int r=0;
        if(inst.mulDiv){
            switch (inst.func3){
                case 0b000://MUL
                    r= (rs1 * rs2);
                    break;
                case 0b001://MULH
                    r= (int)(((long)rs1 * (long)rs2)>>32);
                    break;
                case 0b010://MULHSU
                    r= (int)(((long)rs1 * Integer.toUnsignedLong(rs2)) >> 32);
                    break;
                case 0b011://MULHU
                    r= (int)((Integer.toUnsignedLong(rs1) * Integer.toUnsignedLong(rs2)) >> 32);
                    break;
                case 0b100://DIV
                    if(rs1==Integer.MIN_VALUE && rs2==-1){
                        r=rs1;
                        break;
                    }
                    if(rs2!=0){
                        r=rs1/rs2;
                    }
                    break;
                case 0b101://DIVU
                    if(rs2!=0){
                        r=Integer.divideUnsigned(rs1,rs2);
                    }
                    break;
                case 0b110://REM
                    if(rs2!=0){
                        r=rs1%rs2;
                    }
                    break;
                case 0b111://REMU
                    if(rs2!=0){
                        r=Integer.remainderUnsigned(rs1, rs2);
                    }
                    break;
            }
        }
        else{
            switch (inst.func3){
                case 0b000: //ADD SUB
                    if(inst.func7 & !inst.imm){
                        r=rs1-rs2;
                    }
                    else{
                        r=rs1+rs2;
                    }
                    break;
                case 0b001: //SLL
                    r=rs1<<rs2;
                    break;
                case 0b010: //SLT
                    r=(rs1<rs2)?1:0;
                    break;
                case 0b011: //SLTU
                    r=(Integer.toUnsignedLong(rs1)<Integer.toUnsignedLong(rs2))?1:0;
                    break;
                case 0b100: //XOR
                    r=rs1^rs2;
                    break;
                case 0b101:
                    if(inst.func7) {
                        r = rs1 >> rs2 ;
                    }
                    else{
                        r=rs1 >>> rs2;
                    }
                    break;
                case 0b110: //OR
                    r=rs1|rs2;
                    break;
                case 0b111: //AND
                    r=rs1&rs2;
                    break;
            }
        }
        data.register.setValue(inst.d, r);
    }

    private void iTypeLoad(InstanceState state, ProcessorData data) {
        int address= data.getS1()+data.instruction.getImm();
        if(addressInProcessor(state,address>>2)){
            int offset=state.getAttributeValue(FIRST_ADDR);
            int res=data.contents.get((address-offset)>>>2);
            int bs=(address-offset)&0b11;
            res=selectByteData(data,bs,res);
            if(data.ls_PC){
                data.loadPC(res);
                data.intr_enable=true;
                return;}
            data.register.setValue(data.instruction.d, res);
            data.stepPC(4);
            return;
        }
        data.low=true;
        data.opcode=0b10;
    }
    private void iTypeLoadLow(InstanceState state, ProcessorData data) {
        int address=data.getS1()+data.instruction.getImm();
        int bs=address&0b11;
        Value addr = Value.createKnown(b32, address);
        state.setPort(ADDR, addr, delay);
        switch (data.instruction.func3) {
            case 0b000: //LB
            case 0b100: //LBU
                state.setPort(BE, Value.createKnown(b4, 0b1<<bs), delay + 1);
                break;
            case 0b001: //LH
            case 0b101: //LHU
                state.setPort(BE, Value.createKnown(b4, (0b11<<bs)&0b1111), delay + 1);
                break;
            case 0b010: // LW
                state.setPort(BE, Value.createKnown(b4, (0b1111<<bs)&0b1111), delay + 1);
                break;
            default:
                break;
        }
        state.setPort(RD, Value.TRUE, delay+2);
    }

    private void sType(InstanceState state, ProcessorData data) {
        int address=data.getS1()+data.instruction.getImm();
        if(addressInProcessor(state,address>>2)){
            int store=( data.ls_PC) ? data.intrProgramCount : data.getS2();
            int offset=state.getAttributeValue(FIRST_ADDR);
            int old=data.contents.get((address-offset)>>2);
            int func3=data.instruction.func3;
            int bs=address&0b11;
            if(func3==0b000){ //Store Byte
                int mask=0xFFFF_FF00;
                store&=0xFF;
                store = switch (bs) {
                    case 0b01 -> {
                        mask = (mask << 8) | 0xFF;
                        yield store << 8;
                    }
                    case 0b10 -> {
                        mask = (mask << 16) | 0xFFFF;
                        yield store << 16;
                    }
                    case 0b11 -> {
                        mask = 0x00FFFFFF;
                        yield store << 24;
                    }
                    default -> store;
                };
                store=(old&mask)|store;
            }
            else if(func3==0b001){ //Store Half
                int mask=0xFFFF0000;
                store&=0xFFFF;
                if(bs==2){
                    mask=0xFFFF;
                    store=store<<16;
                }
                store|=(old&mask);
            }
            data.contents.set(address>>2, store);
            data.stepPC(4);
            return;

        }
        data.low=true;
        data.opcode=0b10;
    }
    private void sTypeLow(InstanceState state, ProcessorData data) {
        Value addr=Value.createKnown(b32,data.getS1() + data.instruction.getImm());
        int bs=addr.toIntValue()&0b11;
        Value d=Value.createKnown(b32,
                (data.ls_PC ? data.intrProgramCount : data.getS2()<<(bs<<3)));
        state.setPort(ADDR, addr, delay);
        state.setPort(DATA,d,delay);
        switch (data.instruction.func3) {
            case 0b000: //SB
                state.setPort(BE, Value.createKnown(b4, 0b1<<bs), delay + 1);
                break;
            case 0b001: //SH
                state.setPort(BE, Value.createKnown(b4, (0b11<<bs)&0b1111), delay + 1);
                break;
            case 0b010: // SW
                state.setPort(BE, Value.createKnown(b4, (0b1111<<bs)&0b1111), delay + 1);
                break;
            default:
                break;
        }
        state.setPort(WR, Value.TRUE, delay+2);
    }

    private void bType(ProcessorData data) {
        int Imm = data.instruction.getImm();
        switch(data.instruction.func3){
            case 0b000: // BEQ
                data.stepPC((Objects.equals(data.getS1(), data.getS2()))? Imm : 4);
                break;
            case 0b001: // BNE
                data.stepPC((Objects.equals(data.getS1(), data.getS2()))? 4 : Imm);
                break;
            case 0b100: // BLT
                data.stepPC((data.getS1()< data.getS2())? Imm : 4);
                break;
            case 0b101: // BGE
                data.stepPC((data.getS1()>= data.getS2())? Imm : 4);
                break;
            case 0b110: //BLTU
                data.stepPC((Integer.toUnsignedLong(data.getS1()) < Integer.toUnsignedLong(data.getS2()))? Imm : 4);
                break;
            case 0b111: //BGEU
                data.stepPC((Integer.toUnsignedLong(data.getS1()) >= Integer.toUnsignedLong(data.getS2()))? Imm : 4);
                break;
        }
    }

    private void iTypeEcall() {

    }

    private boolean addressInProcessor(InstanceState state, int address){
        if(state.getAttributeValue(ENABLE_MEMORY)==DISABLE){
            return false;
        }
        int size=(1<<state.getAttributeValue(ADDR_ATTR).getWidth());
        int start=state.getAttributeValue(FIRST_ADDR);
        return(address >=start & address<start+size);
    }
    public String memorySizeToString(int size){
        String text;
        if(size<10){
            text=(int)Math.pow(2,size)+"B";
        }
        else if(size<20){
            text=(int)Math.pow(2,size-10)+"KB";
        }
        else{
            text=(int)Math.pow(2,size-20)+"MB";
        }
        return text;
    }
    private static class ContentsAttribute extends Attribute<DataContents> {
        public ContentsAttribute() {
            super("contents", Strings.getter("processorContentsAttr"));
        }

        @Override
        public java.awt.Component getCellEditor(Window source, DataContents value) {
            if (source instanceof Frame) {
                Project proj = ((Frame) source).getProject();
                ProcessorAttributes.register(value, proj);
            }
            ContentsCell ret = new ContentsCell(source, value);
            ret.mouseClicked(null);
            return ret;
        }

        @Override
        public String toDisplayString(DataContents value)
        {
            return Strings.get("processorContentsValue");
        }

        @Override
        public String toStandardString(DataContents state) {
            int addr = state.getLogLength();
            int data = state.getWidth();
            StringWriter ret = new StringWriter();
            ret.write("addr/data: " + addr + " " + data + "\n");
            try {
                HexFile.save(ret, state);
            } catch (IOException ignored) { }
            return ret.toString();
        }

        @Override
        public DataContents parse(String value) {
            var lineBreak = value.indexOf('\n');
            String first = lineBreak < 0 ? value : value.substring(0, lineBreak);
            String rest = lineBreak < 0 ? "" : value.substring(lineBreak + 1);
            StringTokenizer toks = new StringTokenizer(first);
            try {
                String header = toks.nextToken();
                if (!header.equals("addr/data:")) return null;
                int addr = Integer.parseInt(toks.nextToken());
                int data = Integer.parseInt(toks.nextToken());
                DataContents ret = DataContents.create(addr, data);
                HexFile.open(ret, new StringReader(rest));
                return ret;
            } catch (IOException | NumberFormatException | NoSuchElementException e) {
                return null;
            }
        }
    }

    private static class ContentsCell extends JLabel
            implements MouseListener {
        Window source;
        DataContents contents;

        ContentsCell(Window source, DataContents contents) {
            super(Strings.get("processorContentsValue"));
            this.source = source;
            this.contents = contents;
            addMouseListener(this);
        }

        public void mouseClicked(MouseEvent e) {
            if (contents == null) return;
            Project proj = source instanceof com.cburch.logisim.gui.main.Frame ? ((Frame) source).getProject() : null;
            HexFrame frame = ProcessorAttributes.getHexFrame(contents, proj);
            frame.setVisible(true);
            frame.toFront();
        }

        public void mousePressed(MouseEvent e) { }

        public void mouseReleased(MouseEvent e) { }

        public void mouseEntered(MouseEvent e) { }

        public void mouseExited(MouseEvent e) { }
    }
    @Override
    protected Object getInstanceFeature(Instance instance,Object key){
        if(key == MenuExtender.class) return new ProcessorMenu(this,instance);
        return super.getInstanceFeature(instance,key);
    }
}
