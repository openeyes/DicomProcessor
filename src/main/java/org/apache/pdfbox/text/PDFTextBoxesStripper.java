package org.apache.pdfbox.text;

import org.apache.pdfbox.pdmodel.PDPage;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;

/**
 * This will extract PDFTextBoxes from a PDPage.
 *
 * @author Adrian Brenton
 */
public class PDFTextBoxesStripper extends PDFTextStripper{
    private List<PDFTextBox> pdfTextBoxes;
    private PDPage pdfPage;

    /**
     * Constructor.
     * @throws IOException If there is an error loading properties.
     */
    public PDFTextBoxesStripper() throws IOException {
        super.setShouldSeparateByBeads(false);
    }

    /**
     * Extract the PDFTextBoxes on page and store in member variable pdfTextBoxes
     *
     * @param page The PDPage that will be searched for PDFTextBoxes
     * @throws IOException if processPage(page) encounters an error processing the page
     */
    public void extractPDFTextBoxes(PDPage page) throws IOException {
        setStartPage(getCurrentPageNo());
        setEndPage(getCurrentPageNo());
        if( page.hasContents() )
        {
            pdfTextBoxes = processPage(page);
        }
    }

    /**
     * Getter got pdfTextBoxes member variable
     *
     * @return pdfTextBoxes member variable
     */
    public List<PDFTextBox> getPdfTextBoxes(){
        return pdfTextBoxes;
    }

    /**
     * Gets the PDFTextBox at the given index in the member variable PDF
     *
     * @param index The integer index of the desired PDFTextBox
     * @return pdfTextBoxes member variable
     */
    public PDFTextBox getPDFTextBox(int index){
        return pdfTextBoxes.get(index);
    }

    /**
     * This will print the processed page text to the output stream.
     *
     * @throws IOException If there is an error writing the text.
     */
    @Override
    protected void writePage() throws IOException
    {
        output = new StringWriter();
        super.writePage();
    }

}
