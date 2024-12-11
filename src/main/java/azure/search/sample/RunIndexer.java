package azure.search.sample;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.indexes.SearchIndexerClient;
import com.azure.search.documents.indexes.SearchIndexerClientBuilder;
import com.azure.search.documents.indexes.models.IndexerExecutionStatus;
import com.azure.search.documents.indexes.models.SearchIndexerStatus;

public class RunIndexer {
    public static void main(String[] args) {
        var searchServiceEndpoint = Constraints.SERVICE_ENDPOINT;
        var adminKey = new AzureKeyCredential(Constraints.KEY);
        final String indexName = "resumeblob-index";
        final String indexerName = "azureblob-indexer";

        SearchIndexerClient searchIndexerClient = new SearchIndexerClientBuilder()
                .endpoint(searchServiceEndpoint)
                .credential(adminKey)
                .buildClient();

        try {
            runIndexer(indexerName, searchIndexerClient);

            SearchClient searchClient = new SearchClientBuilder()
                    .endpoint(searchServiceEndpoint)
                    .credential(adminKey)
                    .indexName(indexName)
                    .buildClient();

            System.out.println("doc count = " + searchClient.getDocumentCount());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runIndexer(String indexerName, SearchIndexerClient sic) throws InterruptedException {
        
        sic.runIndexer(indexerName);

        long startTime = System.currentTimeMillis();
        long SLEEP_TIME = 1000;
        for(int i=0;i<60; i++) {
            SearchIndexerStatus status = sic.getIndexerStatus(indexerName);
            System.out.println("Polling Indexer status: " + status.getStatus());
            if (status.getLastResult()!=null) {
                IndexerExecutionStatus lastExecutionStatus = status.getLastResult().getStatus();
                System.out.println("Last execution: " + lastExecutionStatus);

                if( lastExecutionStatus == IndexerExecutionStatus.RESET) {
                    System.out.println("Indexer execution failed");
                    break;
                } else if (lastExecutionStatus == IndexerExecutionStatus.SUCCESS) {
                    System.out.println("Indexer execution succeeded");
                    break;
                }
                Thread.sleep(SLEEP_TIME);
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Indexer execution time: " + (endTime - startTime) + "ms");

    }

}
