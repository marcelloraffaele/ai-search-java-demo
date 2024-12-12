package azure.search.sample.utils;


import java.io.File;
import java.util.List;


public interface PDFParser {
    
    public List<Page> parse(File file);
    public List<Page> parse(byte[] content);
}