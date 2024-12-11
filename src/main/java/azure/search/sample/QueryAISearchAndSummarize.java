package azure.search.sample;

import java.util.ArrayList;
import java.util.List;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.Context;
import com.azure.search.documents.SearchClient;
import com.azure.search.documents.SearchClientBuilder;
import com.azure.search.documents.models.SearchOptions;
import com.azure.search.documents.models.SearchResult;
import com.azure.search.documents.util.AutocompletePagedIterable;
import com.azure.search.documents.util.SearchPagedIterable;

import azure.search.sample.resume.Resume;
import io.github.cdimascio.dotenv.Dotenv;

public class QueryAISearchAndSummarize {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String SEARCH_SERVICE_ENDPOINT = dotenv.get("AZURE_AI_SEARCH_ENDPOINT");
    private static final String SEARCH_API_KEY = dotenv.get("AZURE_AI_SEARCH_API_KEY");
    private static final String INDEX_NAME = "resumeblob-custom-push-index";

    public static void main(String[] args) {

        SearchClient searchClient = new SearchClientBuilder()
                .endpoint(SEARCH_SERVICE_ENDPOINT)
                .credential(new AzureKeyCredential(SEARCH_API_KEY))
                .indexName(INDEX_NAME)
                .buildClient();

        // Call the RunQueries method to invoke a series of queries
        // System.out.println("Starting queries...\n");
        // RunQueries(searchClient);

        SearchOptions options = new SearchOptions();
        options = new SearchOptions();
        options.setFilter("");
        options.setOrderBy("");
        options.setSelect("Id", "FileName", "Content");

        // searchClient.search("\"FileName\": \"ux-designer-mick-red.docx\"", options,
        // Context.NONE)
        // .iterator().forEachRemaining(result -> {
        // Resume r = result.getDocument(Resume.class);
        //
        // System.out.println( result.getScore() + " -> " + r.filename);
        // });;

        SearchResult result = searchClient.search("\"FileName\":  \"ux-designer-mick-red.docx\"", options, Context.NONE)
                .stream().findFirst().get();
        if(result == null) {
            System.out.println("No resume found with the filename 'ux-designer-mick-red.docx'");
            return;
        }
        Resume resume = result.getDocument(Resume.class);
        double score = result.getScore();
        
        //print fount the following resume with a score of x
        System.out.println("Found the following resume with a score of " + score);
        System.out.println(resume);

        // call openai api to summarize the content of the resume
        OpenAIAsyncClient oaiClient = new OpenAIClientBuilder()
                .credential(new AzureKeyCredential(dotenv.get("AZURE_OPENAI_API_KEY")))
                .endpoint(dotenv.get("AZURE_OPENAI_ENDPOINT"))
                .buildAsyncClient();

        List<ChatRequestMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant. You will help me to summarize a document"));
        chatMessages.add(new ChatRequestUserMessage("Please summarize this document: --- " + resume.content + " --- using only 100 words ?"));

        String model = dotenv.get("AZURE_OPENAI_CHAT_DEPLOYMENT_NAME");
        ChatCompletions chatCompletions = oaiClient.getChatCompletions( model, new ChatCompletionsOptions(chatMessages)).block();

        for (ChatChoice choice : chatCompletions.getChoices()) {
            ChatResponseMessage message = choice.getMessage();
            System.out.println("");
            System.out.println("Summary:");
            System.out.println(message.getContent());
        }

        // End the program
        System.out.println("Complete.\n");

    }

    // Run queries, use WriteDocuments to print output
    private static void RunQueries(SearchClient searchClient) {
        // Query 1
        System.out
                .println("Query #1: Search on empty term '*' to return all documents, showing a subset of fields...\n");

        SearchOptions options = new SearchOptions();
        options.setIncludeTotalCount(true);
        options.setFilter("");
        options.setOrderBy("");
        options.setSelect("Id", "FileName");

        WriteSearchResults(searchClient.search("*", options, Context.NONE));

        // Query 2: search Resume of filename ends with .pdf
        System.out.println("Query #2: Search on 'pdf' to return all documents, showing a subset of fields...\n");

        options = new SearchOptions();
        options.setFilter("");
        options.setOrderBy("");
        options.setSelect("Id", "FileName");

        WriteSearchResults(searchClient.search(".pdf", options, Context.NONE));

        // Query 3// search Resume of filename equal to "Classic UIUX designer cover
        // letter.docx"
        System.out.println(
                "Query #3: Search on 'Classic UIUX designer cover letter.docx' to return all documents, showing a subset of fields...\n");

        options = new SearchOptions();
        options.setFilter("");
        options.setOrderBy("");
        options.setSelect("Id", "FileName");

        WriteSearchResults(searchClient.search("Classic UIUX designer cover letter.docx", options, Context.NONE));

    }

    // Write search results to console
    private static void WriteSearchResults(SearchPagedIterable searchResults) {
        searchResults.iterator().forEachRemaining(result -> {
            Resume r = result.getDocument(Resume.class);
            System.out.println(r);
        });

        System.out.println();
    }

    // Write autocomplete results to console
    private static void WriteAutocompleteResults(AutocompletePagedIterable autocompleteResults) {
        autocompleteResults.iterator().forEachRemaining(result -> {
            String text = result.getText();
            System.out.println(text);
        });

        System.out.println();
    }

}
