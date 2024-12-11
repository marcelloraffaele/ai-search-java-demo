package azure.search.sample;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import io.github.cdimascio.dotenv.Dotenv;

public class BlobStorageTest {
    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.load();

        String BLOB_CONNECTION_STRING = dotenv.get("BLOB_CONNECTION_STRING");
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(BLOB_CONNECTION_STRING)
                .buildClient();

        String CONTAINER_NAME = "resume";

        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(CONTAINER_NAME);
        
        containerClient.listBlobs().forEach(blobItem -> {
            System.out.println("Name: " + blobItem.getName() + ", Snapshot: " + blobItem.getSnapshot());
        });


    }
}
