package azure.search.sample;

import com.azure.storage.blob.*;
import azure.search.sample.resume.Resume;
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

public class PushToIndexBlob {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String BLOB_CONNECTION_STRING = dotenv.get("BLOB_CONNECTION_STRING");
    private static final String CONTAINER_NAME = "resume";
    private static final String SEARCH_SERVICE_ENDPOINT = dotenv.get("AZURE_AI_SEARCH_ENDPOINT");
    private static final String SEARCH_API_KEY = dotenv.get("AZURE_AI_SEARCH_API_KEY");
    private static final String INDEX_NAME = "resumeblob-custom-push-index";

    // this main function is responsible for creating a search index and pushing the content of the resumes 
    // in the blob storage to the search index
    public static void main(String[] args) {

        SearchIndexClient searchIndexClient = new SearchIndexClientBuilder()
                .endpoint(SEARCH_SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .buildClient();
                
        searchIndexClient
                .createOrUpdateIndex(
                        new SearchIndex(INDEX_NAME, SearchIndexClient.buildSearchFields(Resume.class, null))
                                .setSuggesters(new SearchSuggester("sg", Arrays.asList("Title", "FileName", "Name", "Content"))));

        SearchClient searchClient = new SearchClientBuilder()
                .endpoint(SEARCH_SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .indexName(INDEX_NAME)
                .buildClient();

        // Create a client
        DocumentAnalysisClient documentAnalysisClient = new DocumentAnalysisClientBuilder()
            .endpoint( dotenv.get("DOCUMENTINTELLIGENCE_ENDPOINT") )
            .credential(new AzureKeyCredential(dotenv.get("DOCUMENTINTELLIGENCE_API_KEY")))
            .buildClient();
        
        //load file from blob storage
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(BLOB_CONNECTION_STRING)
                .buildClient();

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        System.out.println("Listing blobs...");

        List<Resume> resumeList = new ArrayList<Resume>();

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
                    Resume resume = parsePdfFile(filename,data, documentAnalysisClient);
                    //print the resume content
                    //System.out.println(resume);

                    resumeList.add(resume);                

                } catch (Exception e) {
                    System.out.println("Failed to index documents: " + e.getMessage());
                }

            }

        }); // for

        if(!resumeList.isEmpty()){
            try {

                IndexDocumentsBatch<Resume> batch = new IndexDocumentsBatch<Resume>();
                batch.addMergeOrUploadActions(resumeList);
                IndexDocumentsResult result = searchClient.indexDocuments(batch);

                System.out.println("Indexed documents: " + result.getResults().size());

            } catch (Exception e) {
                System.out.println("Failed to index documents: " + e.getMessage());
            }
        }

    }

    private static Resume parsePdfFile(String filename, byte[] data, DocumentAnalysisClient dac) {
        String modelId = "prebuilt-read";

        // parse the file using the Form Recognizer client
        com.azure.ai.formrecognizer.documentanalysis.models.AnalyzeResult analyzeResult = dac
                .beginAnalyzeDocument(modelId, BinaryData.fromBytes(data)).getFinalResult();

        String id = generateMD5(filename);

        Resume resume = new Resume();
        resume.Id = id;
        resume.filename = filename;
        resume.content = analyzeResult.getContent();

        return resume;

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