package cn.bugstack.xfg.dev.tech.trigger.http;

import cn.bugstack.xfg.dev.tech.api.IRAGService;
import cn.bugstack.xfg.dev.tech.api.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/rag/")
public class RAGController implements IRAGService {
    @Resource
    private OllamaChatClient ollamaChatClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter; // ✂️ 剪刀：用来把长文本切碎
    @Resource
    private SimpleVectorStore simpleVectorStore;
    @Resource
    private PgVectorStore pgVectorStore; // 🧠 长期记忆：基于 PostgreSQL 的向量数据库
    @Resource
    private RedissonClient redissonClient; // ⚡️ 快速便签：Redis 客户端，用来存一些简单的列表
    //TikaDocumentReader 为什么没用 @Resource？
    //
    //因为它是处理具体文件的，每个文件都不一样，不能单例复用，所以必须在方法里 new 出来。
    @RequestMapping(value = "query_rag_tag_list",method = RequestMethod.GET)
    @Override
    public Response<List<String>> queryRagTagList() {
        RList<String> elements = redissonClient.getList("ragTag");
        return Response.<List<String>>builder()
                .code("0000")
                .msg("调用成功")
                .data(elements)
                .build();
    }

    @PostMapping(value = "file/upload")
    @Override
    // 1. 定义接口
    // @RequestParam String ragTag: 接收前端传来的标签，比如 "Java教程"
    // @RequestParam("file") List<MultipartFile> files: 接收前端上传的一个或多个文件
    public Response<String> uploadFile(@RequestParam String ragTag,@RequestParam("file") List<MultipartFile> files) {
        log.info("上传知识库开始{}", ragTag);
        // 2. 遍历文件
        // 这是一个 Enhanced For-Loop (增强型 for 循环)
        for(MultipartFile file : files) {
            // 3. 提取文本 (Extract)
            // file.getResource() 拿到了文件的输入流
            // Tika 是一个强大的库，它能自动识别 PDF、Word、TXT，把它们统一变成 String
            TikaDocumentReader  documentReader = new TikaDocumentReader(file.getResource());
            List<Document> documents = documentReader.get();
            // 4. 文本切分 (Transform)
            // 语法点：.apply()
            // TokenTextSplitter 实现了一个 Java 函数式接口 (Function)。
            // 它的作用是：把一大坨 documents，按 token 限制（比如 500词一跨）切成更多的小 documents
            List<Document> documentSplitterList = tokenTextSplitter.apply(documents);
            // 5. 补充元数据 (Metadata Enrichment) - 重点语法！
            // 这里使用了 Lambda 表达式 (->)
            // 翻译：对于 documentSplitterList 里的每一个 document，都执行后面的操作。
            // 操作内容：在它的元数据小本本里，记下 "knowledge" = "Java教程"。
            // 目的：以后你搜 "Java" 的时候，能根据这个标签过滤数据。
            documents.forEach(document -> document.getMetadata().put("knowledge",ragTag));
            documentSplitterList.forEach(document -> document.getMetadata().put("knowledge",ragTag));
            // 6. 存入向量库 (Load)
            // 语法点：.accept()
            // PgVectorStore 实现了 Consumer 接口。
            // 这行代码最神奇：它会把你的文本，通过 Embedding 模型变成向量（一堆数字），然后存进 Postgres 数据库。
            pgVectorStore.accept(documentSplitterList);
            // 7. 更新 Redis 缓存
            // 这里的逻辑是：为了让前端能有个下拉框选 "Java教程"，我们把这个 tag 存到 Redis 的一个 List 里。
            RList<String> elements = redissonClient.getList("ragTag");
            if(!elements.contains(ragTag)) {
                elements.add(ragTag);// 如果 List 里没有这个标签，就加进去
            }
            log.info("上传知识库完成{}", ragTag);
        }
        // 8. 返回结果 (Builder 模式)
        // 这种链式调用写法 (.builder().code().msg().build()) 是由 Lombok 的 @Builder 生成的。
        // 也就是我们上一个问题讨论过的，非常优雅的创建对象方式。
        return Response.<String>builder().code("0000").msg("调用成功").build();
    }
}
