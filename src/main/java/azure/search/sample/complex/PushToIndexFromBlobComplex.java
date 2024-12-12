package azure.search.sample.complex;

import java.io.ByteArrayOutputStream;

import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClient;
import com.azure.ai.formrecognizer.documentanalysis.DocumentAnalysisClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import azure.search.sample.utils.AzureOpenAIEmbeddingService;
import azure.search.sample.utils.DocumentIntelligencePDFParser;
import azure.search.sample.utils.DocumentProcessor;
import azure.search.sample.utils.PDFParser;
import azure.search.sample.utils.SearchIndexManager;
import azure.search.sample.utils.TextEmbeddingsService;
import azure.search.sample.utils.TextSplitter;
import io.github.cdimascio.dotenv.Dotenv;

public class PushToIndexFromBlobComplex {

    // this main function is responsible for creating a search index and pushing the content of the resumes 
    // in the blob storage to the search index
    public static void main(String[] args) {

        Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().systemProperties().load();
        final String BLOB_CONNECTION_STRING = System.getProperty("BLOB_CONNECTION_STRING");
        final String CONTAINER_NAME = "contoso";
        final String SEARCH_SERVICE_ENDPOINT = System.getProperty("AZURE_AI_SEARCH_ENDPOINT");
        final String SEARCH_API_KEY = System.getProperty("AZURE_AI_SEARCH_API_KEY");
        final String INDEX_NAME = "documents-complex-index";

        SearchIndexClient searchIndexClient = new SearchIndexClientBuilder()
                .endpoint(SEARCH_SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .buildClient();
                

        SearchClient searchClient = new SearchClientBuilder()
                .endpoint(SEARCH_SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .indexName(INDEX_NAME)
                .buildClient();

        DocumentAnalysisClient documentAnalysisClient = new DocumentAnalysisClientBuilder()
            .endpoint( System.getProperty("DOCUMENTINTELLIGENCE_ENDPOINT") )
            .credential(new AzureKeyCredential(System.getProperty("DOCUMENTINTELLIGENCE_API_KEY")))
            .buildClient();
        
        //-----
        String openAIServiceName="admin-m30tvd5h-westeurope";
        String openAiDeploymentName=System.getProperty("AZURE_OPENAI_EMBEDDING_DEPLOYMENT_NAME");
        AzureKeyCredential credential=new AzureKeyCredential(System.getProperty("AZURE_OPENAI_API_KEY"));
        TextEmbeddingsService embeddingsService = new AzureOpenAIEmbeddingService(openAIServiceName, openAiDeploymentName, credential, false);

        SearchIndexManager searchIndexManager = new SearchIndexManager(searchIndexClient, INDEX_NAME, searchClient, embeddingsService);
        PDFParser pdfParser = new DocumentIntelligencePDFParser(documentAnalysisClient, false);
        TextSplitter textSplitter = new TextSplitter(false);
        final DocumentProcessor documentProcessor = new DocumentProcessor(searchIndexManager, pdfParser, textSplitter);

        searchIndexManager.createIndex();
        //-----
        
        
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(BLOB_CONNECTION_STRING)
                .buildClient();

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        System.out.println("Listing blobs...");

        
        containerClient.listBlobs().forEach(blobItem -> {
            String filename = blobItem.getName();
            System.out.println("Blob name: " + filename);
            
            if (filename.endsWith(".pdf") /*|| filename.endsWith(".docx")*/) {
                

                // read from blob filename and upload to search index
                BlobClient blobClient = containerClient.getBlobClient(filename);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                blobClient.download(outputStream);
                byte[] data = outputStream.toByteArray();

                // verify if the file is a pdf or a word document
                try {
                    
                    documentProcessor.indexDocumentFromBytes(filename, "", data);


                } catch (Exception e) {
                    System.out.println("Failed to index documents: " + e.getMessage());
                }

            }//if
            
        }); // for

    }


}