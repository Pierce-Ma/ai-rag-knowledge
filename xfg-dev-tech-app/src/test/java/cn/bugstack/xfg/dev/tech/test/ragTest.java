package cn.bugstack.xfg.dev.tech.test;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class ragTest {

    @Resource
    private OllamaChatClient ollamaChatClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private SimpleVectorStore simpleVectorStore;
    @Resource
    private PgVectorStore pgVectorStore;
    @Test
    public void upload(){
        TikaDocumentReader reader = new TikaDocumentReader("./data/file.text");
        List<org.springframework.ai.document.Document> documents = reader.get();
        List<Document> documentSplliterList = tokenTextSplitter.apply(documents);
        documents.forEach(doc -> doc.getMetadata().put("knowledge", "知识库名称"));
        documentSplliterList.forEach(doc -> doc.getMetadata().put("knowledge", "知识库名称"));
        pgVectorStore.accept(documentSplliterList);
        log.info("上传完成");
    }
    @Test
    public void chat() {
        // 1. 模拟用户提问
        String message = "王大瓜，哪年出生";

        // 2. 定义系统提示词 (System Prompt)
        // 这里的核心是 {documents} 占位符，稍后会把查到的资料填进去
        String SYSTEM_PROMPT = """
        Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information
        If unsure, simply state that you don't know.
        Another thing you need to note is that your reply must be in Chinese!
        DOCUMENTS:
            {documents}
        """;

        // 3. 构建搜索请求 (SearchRequest)
        // query(message): 把用户的问题转成向量去搜
        // withTopK(5): 只取匹配度最高的 5 条记录
        // withFilterExpression(...): 过滤条件，只在 "知识库名称" 这个分类里找（对应你上一张图打的标签）
        SearchRequest request = SearchRequest.query(message).withTopK(5).withFilterExpression("knowledge == '知识库名称'");

        // 4. 执行向量检索 (Retrieval)
        // 去 pgVectorStore 数据库里真正执行搜索
        List<Document> documents = pgVectorStore.similaritySearch(request);

        // 5. 提取并拼接资料 (Context Aggregation)
        // 把搜出来的 List<Document> 里的文本内容全部提取出来，拼成一个长字符串
        String documentsCollectors = documents.stream().map(Document::getContent).collect(Collectors.joining());

        // 6. 生成最终提示词 (Augmentation)
        // 把拼好的资料 (documentsCollectors) 填入 SYSTEM_PROMPT 里的 {documents} 占位符
        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));

        // (截图未显示部分：通常接下来就是把这个 ragMessage 发给 chatClient 获得最终回复)
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);
        ChatResponse chatResponse = ollamaChatClient.call(new Prompt(messages, OllamaOptions.create().withModel("deepseek-r1:1.5b")));
        log.info("测试结果:{}",JSON.toJSONString(chatResponse));

    }


}
