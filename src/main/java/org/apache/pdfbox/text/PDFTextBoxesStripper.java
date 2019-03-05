package org.apache.pdfbox.text;

import org.apache.pdfbox.pdmodel.PDPage;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * This will extract PDFTextBoxes from a PDPage.
 *
 * @author Adrian Brenton
 */
public class PDFTextBoxesStripper extends PDFTextStripper{
    /**
     * Constructor.
     * @throws IOException If there is an error loading properties.
     */
    public PDFTextBoxesStripper() throws IOException {
        super.setShouldSeparateByBeads(false);
        output = new StringWriter();
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
            processPage(page);
        }
    }
}
