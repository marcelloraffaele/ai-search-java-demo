package azure.search.sample;

import azure.search.sample.resume.Resume;
import io.github.cdimascio.dotenv.Dotenv;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.*;
import com.azure.search.documents.indexes.SearchIndexClient;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.azure.search.documents.indexes.models.IndexDocumentsBatch;
import com.azure.search.documents.indexes.models.SearchIndex;
import com.azure.search.documents.indexes.models.SearchSuggester;
import com.azure.search.documents.models.*;
import java.util.*;

public class PushToIndexPojo {

    public static void main(String[] args) {
      
        Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().systemProperties().load();
        final String SEARCH_SERVICE_ENDPOINT = System.getProperty("AZURE_AI_SEARCH_ENDPOINT");
        final String SEARCH_API_KEY = System.getProperty("AZURE_AI_SEARCH_API_KEY");
        final String INDEX_NAME = "resumeblob-pojo-push-index";

        SearchIndexClient searchIndexClient = new SearchIndexClientBuilder()
                .endpoint(SEARCH_SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .buildClient();
        searchIndexClient
                .createOrUpdateIndex(
                        new SearchIndex(INDEX_NAME, SearchIndexClient.buildSearchFields(Resume.class, null))
                                .setSuggesters(new SearchSuggester("sg", Arrays.asList("Title", "Name"))));

        SearchClient searchClient = new SearchClientBuilder()
                .endpoint(SEARCH_SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .indexName(INDEX_NAME)
                .buildClient();

         uploadDocumentsFromPojo(searchClient);

    }

    private static void uploadDocumentsFromPojo(SearchClient searchClient) {
        List<Resume> resumeList = new ArrayList<Resume>();
        Resume r1 = new Resume();
        r1.Id = "1";
        r1.filename = "resume1.pdf";
        r1.name = "John Doe";
        r1.title = "Software Engineer";
        r1.address = "123 Main St, Seattle, WA 98101";
        resumeList.add(r1);

        // create another resume
        Resume r2 = new Resume();
        r2.Id = "2";
        r2.filename = "resume2.pdf";
        r2.name = "Michael Smith";
        r2.title = "UX designer";
        r2.address = "456 Elm St, Seattle, WA 98101";
        resumeList.add(r2);

        Resume r3 = new Resume();
        r3.Id = "3";
        r3.content="Marco Giovannini UX designer";
        resumeList.add(r3);

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