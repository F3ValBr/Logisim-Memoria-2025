package com.cburch.logisim.verilog.comp;

public interface WordLvlCell extends VerilogCell {
    /**
     * Returns the number of bits in this word-level cell.
     *
     * @return the number of bits
     */
    public int getBitWidth();

    /**
     * Returns the number of words in this word-level cell.
     *
     * @return the number of words
     */
    public int getWordCount();
}
