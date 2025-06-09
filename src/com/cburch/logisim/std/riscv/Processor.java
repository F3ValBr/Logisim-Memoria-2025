package com.cburch.logisim.std.riscv;

import com.cburch.logisim.data.*;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class Processor extends InstanceFactory {

    //processor attributes
    static final AttributeOption SHOW_REGISTER
            = new AttributeOption("showRegister", Strings.getter("processorShowRegister"));
    static final AttributeOption HIDE_REGISTER
            = new AttributeOption("hideRegister", Strings.getter("processorHideRegister"));
    static final Attribute<AttributeOption> DISPLAY_REGISTER
            = Attributes.forOption("displayRegister",Strings.getter("processorDisplayRegister"),
            new AttributeOption[] { SHOW_REGISTER,HIDE_REGISTER});
    public static final Attribute<Integer> BOOT_ADDR = Attributes.forHexInteger("bootAddress",Strings.getter("bootAddress"));
    public static final Attribute<BitWidth> ADDR_ATTR = Attributes.forBitWidth(
            "addrWidth", Strings.getter("ProcessorAddrWidthAttr"), 6, 24);
    public static Attribute<DataContents> CONTENTS_ATTR = new ContentsAttribute();


    //Port address
    static final int DATA = 0;
    static final int ADDR = 1;
    static final int BE = 2;
    static final int CLK = 3;
    static final int RD = 4;
    static final int WR = 5;
    static final int MEM_INPUTS = 6;

    static final BitWidth b32 = BitWidth.create(32);
    static final BitWidth b4 = BitWidth.create(4);
    static final int delay=10;
    static int multiplier=32;
    public Processor() {
        super("processor", Strings.getter("processorComponent"));
        setIconName("rom.gif");
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
        ps[DATA].setToolTip(Strings.getter("memDataTip"));
        ps[ADDR].setToolTip(Strings.getter("memAddrTip"));
        instance.setPorts(ps);
    }
    @Override
    protected void configureNewInstance(Instance instance) {
        configurePorts(instance);
    }
    @Override
    public AttributeSet createAttributeSet() {
        return new ProcessorAttributes();
    }
    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Bounds bds = painter.getBounds();
        painter.drawBounds();
        painter.drawPort(DATA, Strings.get("DATA"), Direction.WEST);
        painter.drawPort(ADDR, Strings.get("ADDR"), Direction.EAST);
        painter.drawPort(BE, Strings.get("BE"), Direction.EAST);
        painter.drawPort(RD, Strings.get("RD"), Direction.EAST);
        painter.drawPort(WR, Strings.get("WR"), Direction.EAST);
        painter.drawClock(CLK, Direction.NORTH);
        GraphicsUtil.drawCenteredText(g, "Risc-V", bds.getX() + bds.getWidth()
                / 2, bds.getY() + bds.getHeight() / 2);
    }
    @Override
    public void propagate(InstanceState state) {
        ProcessorData data =(ProcessorData) state.getData();
        if(data == null) {//init data
            data=new ProcessorData(state.getAttributeValue(CONTENTS_ATTR),state.getAttributeValue(BOOT_ADDR));
            state.setData(data);
        }
        Value last=data.lastClock;
        data.lastClock=state.getPort(CLK);
        if(data.lastClock==last || last==Value.UNKNOWN) {return;}
        if(data.low){
            propagateLow(state,data,last);
            return;
        }
        for(int i=0;i<multiplier;i++){
            System.out.println(data.programCount);
            getInstruction(data);
            if(data.low){
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
        System.out.println("Low");
        if(!(data.lastClock == Value.FALSE && last == Value.TRUE)){return;}
        state.setPort(WR,Value.FALSE,delay);
        state.setPort(RD,Value.FALSE,delay);
        System.out.println(data.opcode);
        switch (data.opcode) {
            case 0b0:
                goFetch(state, data);
                break;
            case 0b1:
                fetch(state, data);
                data.low = false;
                break;
            case 0b10:
                decodeLoadStore(state, data);
                break;
            //load data
            case  0b0000011:
                Value res = state.getPort(ADDR);
                if (res.isFullyDefined()) {
                    receiveData(res.toIntValue(), data);
                    data.low = false;
                }
        }
    }

    private void decodeLoadStore(InstanceState state, ProcessorData data) {
        data.opcode=data.instruction.opcode;
        if (data.opcode == 0b0000011) {
            iTypeLoadLow(state,data);
        }
    }

    private void getInstruction(ProcessorData data) {
        if( data.programCount < (1<<data.contents.getLogLength())){
            data.setInstruction(data.contents.get(data.programCount));
        }
        else{
            data.low=true;
            data.opcode=0;
        }
    }

    private void goFetch(InstanceState state, ProcessorData data){
        data.opcode=1;
        state.setPort(ADDR,Value.createKnown(b32,data.programCount),delay);
        state.setPort(RD,Value.TRUE,delay+1);
        state.setPort(BE,Value.createKnown(b4,7),delay);
    }
    private void fetch(InstanceState state, ProcessorData data){
        Value inst =state.getPort(DATA);
        if(inst.isFullyDefined()){
            data.setInstruction(inst);
            state.setPort(RD,Value.FALSE,delay);
            execute(state,data);
        }
    }

    private void execute(InstanceState state, ProcessorData data){
        data.opcode=data.instruction.opcode;
        switch (data.opcode){
            case 0b0110011: // ADD / SUB / SLL / SLT / SLTU / XOR / SRL / SRA / OR / AND / MUL / DIV
                System.out.println("Rtype");
                rType(state,data,data.getS1(),data.getS2());
                data.stepPC(4);
                break;
            case 0b1101111: //JAL
                System.out.println("JAL");
                data.register.setValue(data.instruction.d, data.programCount+4);
                data.stepPC(data.instruction.getImmJ());
            case 0b1100111: // JALR
                System.out.println("JALR");
                data.register.setValue(data.instruction.d, data.programCount+4);
                data.stepPCS1(data.instruction.getImmI());
                break;
            case 0b0000011: // LB / LH / LW / LBU / LHU
                System.out.println("LOAD");
                iTypeLoad(state,data);
                break;
            case 0b0010011: // ADDI / SLTI / SLTIU / XORI / ORI / ANDI / SLLI / SRLI / SRAI
                //TODO check immediate ops
                System.out.println("RtypeI");
                rType(state,data,data.getS1(),data.instruction.getImmI());
                data.stepPC(4);
                break;
            default:
                data.stepPC(4);
                break;
        }
    }
    private void receiveData(int res, ProcessorData data){
        int func3=data.instruction.func3;
        if(func3==0b100){res&=0xFF;}
        else if(func3==0b101){res&=0xFFFF;}
        data.register.setValue(data.instruction.d, res);
        data.stepPC(4);
    }
    private void rType(InstanceState state,ProcessorData data,int rs1,int rs2) {
        Instruction inst=data.instruction;
        int r=0;
        if(inst.muldiv){
            switch (inst.func3){
                //TODO make mul div switch
            }
        }
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
                r=rs1<<(rs2&0x1f);
                break;
            case 0b010: //STL
                r=(rs1<rs2)?1:0;
                break;
            case 0b011: //STLU
                r=(Integer.toUnsignedLong(rs1)<Integer.toUnsignedLong(rs2))?1:0;
                break;
            case 0b100:
                r=rs1^rs2;
                break;
            case 0b101:
                if(inst.func7){
                }
        }
        data.register.setValue(inst.d, r);
    }
    private void iTypeLoad(InstanceState state, ProcessorData data) {
        int address=data.getS1()+data.instruction.getImmI();
        if(address < (1<<state.getAttributeValue(ADDR_ATTR).getWidth())){
            int res=data.contents.get((long) address);
            int func3=data.instruction.func3;
            if(func3==0b100){res&=0xFF;}
            else if(func3==0b101){res&=0xFFFF;}
            data.register.setValue(data.instruction.d, res);
            return;
        }
        data.opcode=2;
        data.low=true;
    }
    private void iTypeLoadLow(InstanceState state, ProcessorData data) {
        Value addr = Value.createKnown(b32, data.getS1() + data.instruction.getImmI());
        state.setPort(ADDR, addr, delay);
        switch (data.instruction.func3) {
            case 0b000: //LB
            case 0b100: //LBU
                state.setPort(BE, Value.createKnown(b4, 1), delay + 1);
                break;
            case 0b001: //LH
            case 0b101: //LHU
                state.setPort(BE, Value.createKnown(b4, 3), delay + 1);
                break;
            case 0b010: // LW
                state.setPort(BE, Value.createKnown(b4, 7), delay + 1);
                break;
            default:
                break;
        }
        state.setPort(RD, Value.TRUE, delay+2);
    }
    private static class ContentsAttribute extends Attribute<DataContents> {
        public ContentsAttribute() {
            super("contents", Strings.getter("ProcessorContentsAttr"));
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
        public String toDisplayString(DataContents value) {
            return Strings.get("ProcessorContentsValue");
        }

        @Override
        public String toStandardString(DataContents state) {
            int addr = state.getLogLength();
            int data = state.getWidth();
            StringWriter ret = new StringWriter();
            ret.write("addr/data: " + addr + " " + data + "\n");
            try {
                HexFile.save(ret, state);
            } catch (IOException e) { }
            return ret.toString();
        }

        @Override
        public DataContents parse(String value) {
            int lineBreak = value.indexOf('\n');
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
            } catch (IOException e) {
                return null;
            } catch (NumberFormatException e) {
                return null;
            } catch (NoSuchElementException e) {
                return null;
            }
        }
    }

    private static class ContentsCell extends JLabel
            implements MouseListener {
        Window source;
        DataContents contents;

        ContentsCell(Window source, DataContents contents) {
            super(Strings.get("romContentsValue"));
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
}

