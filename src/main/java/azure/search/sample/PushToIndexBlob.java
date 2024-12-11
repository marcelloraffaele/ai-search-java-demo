package azure.search.sample;

import com.azure.storage.blob.*;
import azure.search.sample.resume.Resume;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.*;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.azure.search.documents.indexes.models.IndexDocumentsBatch;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.azure.search.documents.indexes.models.SearchSuggester;
import com.azure.search.documents.models.*;
import java.io.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PushToIndexBlob {
    private static final String BLOB_CONNECTION_STRING = "<your-connection-string>";
    private static final String CONTAINER_NAME = "resume";
    private static final String SEARCH_SERVICE_ENDPOINT = Constraints.SERVICE_ENDPOINT;
    private static final String SEARCH_API_KEY = Constraints.KEY;
    private static final String INDEX_NAME = "resumeblob-custom-push-index";

    public static void main(String[] args) {

        SearchIndexClient searchIndexClient = new SearchIndexClientBuilder()
                .endpoint(Constraints.SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(Constraints.KEY))
                .buildClient();
        searchIndexClient
                .createOrUpdateIndex(
                        new SearchIndex(INDEX_NAME, SearchIndexClient.buildSearchFields(Resume.class, null))
                                .setSuggesters(new SearchSuggester("sg", Arrays.asList("Title", "Name", "Content"))));

        SearchClient searchClient = new SearchClientBuilder()
                .endpoint(SEARCH_SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .indexName(INDEX_NAME)
                .buildClient();

        uploadDocumentsFromBlob(searchClient);

    }

    private static void uploadDocumentsFromBlob(SearchClient searchClient) {

        
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(BLOB_CONNECTION_STRING)
                .buildClient();

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        System.out.println("Listing blobs...");

        List<Resume> resumeList = new ArrayList<Resume>();
        
        List<String> blobNames = Arrays.asList("Classic UIUX designer cover letter.docx",
            "Classic UIUX designer cover letter.pdf",
                        "Modern hospitality resume.pdf",
                        "Modern nursing resume.docx",
                        "Modern nursing resume.pdf",
                        "Paralegal resume.pdf",
                        "ux-designer-mick-red.docx",
                        "ux-ela-green.docx");
        for (String filename: blobNames) {
            //String fileName=blobItem.getName();
            System.out.println("Blob name: " + filename);

            // read from blob filename and upload to search index
            BlobClient blobClient = containerClient.getBlobClient(filename);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.download(outputStream);
            byte[] data = outputStream.toByteArray();

            // verify if the file is a pdf or a word document
            try {

                if (filename.endsWith(".pdf")) {
                    // parse the pdf file to txt
                    String parsed = parsePdfFile(data);
                
                    Resume resume = new Resume();
                    resume.Id = UUID.randomUUID().toString();
                    resume.filename = filename;
                    resume.content = parsed;
                    resumeList.add(resume);                    
                }

            } catch (Exception e) {
                System.out.println("Failed to index documents: " + e.getMessage());
            }

        }//for
        

        

        try {

            IndexDocumentsBatch<Resume> batch = new IndexDocumentsBatch<Resume>();
            batch.addMergeOrUploadActions(resumeList);
            IndexDocumentsResult result = searchClient.indexDocuments(batch);

            System.out.println("Indexed documents: " + result.getResults().size());

        } catch (Exception e) {
            System.out.println("Failed to index documents: " + e.getMessage());
        }

    }

    private static String parsePdfFile(byte[] data) {
        try {
            PDDocument document = PDDocument.load(data);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            document.close();
            return text;
        } catch (Exception e) {
            System.out.println("Failed to parse pdf file: " + e.getMessage());
            return null;
        }
    }

}