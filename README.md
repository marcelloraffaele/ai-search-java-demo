# ai-search-java-demo

## How to run this example
You need to create a `.env` file in the root folder of the project following the `.template.env file.` You need to replace the `your_api_key` with your own API key.

```bash
AZURE_OPENAI_API_KEY=
AZURE_OPENAI_ENDPOINT=
AZURE_OPENAI_CHAT_DEPLOYMENT_NAME="gpt-4o"
AZURE_OPENAI_TEXT_DEPLOYMENT_NAME="gpt-4o"
AZURE_OPENAI_EMBEDDING_DEPLOYMENT_NAME=""
AZURE_OPENAI_API_VERSION="2024-08-01-preview"
AZURE_AI_SEARCH_API_KEY=""
AZURE_AI_SEARCH_ENDPOINT="https://xxxx.search.windows.net"
DOCUMENTINTELLIGENCE_ENDPOINT="https://xxxxx.cognitiveservices.azure.com/"
DOCUMENTINTELLIGENCE_API_KEY=""
BLOB_CONNECTION_STRING=""
```
In alternative you can set the properties when run the java application:
    
```bash
java -DAZURE_AI_SEARCH_ENDPOINT="your_endpoint" -DAZURE_AI_SEARCH_API_KEY="your_api_key" -cp your_classpath PushToIndexPojo
```