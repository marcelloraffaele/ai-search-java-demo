package azure.search.sample.documents;

import com.azure.storage.blob.*;
import io.github.cdimascio.dotenv.Dotenv;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.search.documents.*;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.azure.search.documents.indexes.models.IndexDocumentsBatch;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.azure.search.documents.indexes.models.SearchSuggester;
import com.azure.search.documents.models.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class PushToIndexFromBlob {

    // this main function is responsible for creating a search index and pushing the content of the resumes 
    // in the blob storage to the search index
    public static void main(String[] args) {

        Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().systemProperties().load();
        final String BLOB_CONNECTION_STRING = System.getProperty("BLOB_CONNECTION_STRING");
        final String CONTAINER_NAME = "resume";
        final String SEARCH_SERVICE_ENDPOINT = System.getProperty("AZURE_AI_SEARCH_ENDPOINT");
        final String SEARCH_API_KEY = System.getProperty("AZURE_AI_SEARCH_API_KEY");
        final String INDEX_NAME = "documents-index";

        SearchIndexClient searchIndexClient = new SearchIndexClientBuilder()
                .endpoint(SEARCH_SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .buildClient();
                
        searchIndexClient
                .createOrUpdateIndex(
                        new SearchIndex(INDEX_NAME, SearchIndexClient.buildSearchFields(Document.class, null))
                                .setSuggesters(new SearchSuggester("sg", Arrays.asList("FileName", "Content"))));

        SearchClient searchClient = new SearchClientBuilder()
                .endpoint(SEARCH_SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .indexName(INDEX_NAME)
                .buildClient();

        DocumentAnalysisClient documentAnalysisClient = new DocumentAnalysisClientBuilder()
            .endpoint( System.getProperty("DOCUMENTINTELLIGENCE_ENDPOINT") )
            .credential(new AzureKeyCredential(System.getProperty("DOCUMENTINTELLIGENCE_API_KEY")))
            .buildClient();
        
        
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(BLOB_CONNECTION_STRING)
                .buildClient();

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        System.out.println("Listing blobs...");

        List<Document> resumeList = new ArrayList<>();

        containerClient.listBlobs().forEach(blobItem -> {
            String filename = blobItem.getName();
            if (filename.endsWith(".pdf") || filename.endsWith(".docx")) {
                System.out.println("Blob name: " + filename);

                // read from blob filename and upload to search index
                BlobClient blobClient = containerClient.getBlobClient(filename);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                blobClient.download(outputStream);
                byte[] data = outputStream.toByteArray();

                // verify if the file is a pdf or a word document
                try {
                    
                    // parse the pdf file to txt
                    Document d = parsePdfFile(filename,data, documentAnalysisClient);
                    resumeList.add(d);                

                } catch (Exception e) {
                    System.out.println("Failed to index documents: " + e.getMessage());
                }

            }

        }); // for

        if(!resumeList.isEmpty()){
            try {

                IndexDocumentsBatch<Document> batch = new IndexDocumentsBatch<>();
                batch.addMergeOrUploadActions(resumeList);
                IndexDocumentsResult result = searchClient.indexDocuments(batch);

                System.out.println("Indexed documents: " + result.getResults().size());

            } catch (Exception e) {
                System.out.println("Failed to index documents: " + e.getMessage());
            }
        }

    }

    private static Document parsePdfFile(String filename, byte[] data, DocumentAnalysisClient dac) {
        String modelId = "prebuilt-read";

        // parse the file using the Form Recognizer client
        com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult analyzeResult = dac
                .beginAnalyzeDocument(modelId, BinaryData.fromBytes(data)).getFinalResult();

        String id = generateMD5(filename);

        Document d = new Document();
        d.Id = id;
        d.filename = filename;
        d.content = analyzeResult.getContent();

        return d;

    }

    private static String generateMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}