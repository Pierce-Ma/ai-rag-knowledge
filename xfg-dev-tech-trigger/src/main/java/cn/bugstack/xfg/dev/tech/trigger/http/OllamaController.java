package cn.bugstack.xfg.dev.tech.trigger.http;

import cn.bugstack.xfg.dev.tech.api.IAiService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/ollama/")
/**
 * 这里为什么是用Controller来实现 Interface 而不是MVC常见的 Service来实现呢？
 * Controller 直接实现 Interface 是一种特定的设计模式，通常用于微服务或**DDD（领域驱动设计）**的架构中。
 * 这是一个“API 契约”模式 (Contract-First)
 * Interface 定义在：cn.bugstack...api 包里。
 *
 * Controller 定义在：cn.bugstack...trigger 包里。
 * 目的： api 模块通常会被打包成一个 jar 包，提供给外部的调用方（比如其他的微服务，或者 Feign Client）使用。
 *
 * 对于调用方：他们只看得到 IAIService 这个接口，知道怎么调用。
 *
 * 对于提供方（Controller）：通过 implements IAIService，强制 Controller 必须严格遵守 api 中定义的接口签名（方法名、参数、返回值）。
 *
 * 这样能保证：你对外暴露的 HTTP 接口，和你给别人用的 SDK 接口是完全一致的。 防止你改了 Controller 的代码，却忘了改给别人的 SDK 接口。
 */
public class OllamaController implements IAiService {

    /**
     * @Autowired (Spring 亲儿子)
     *
     * 默认行为：按类型 (By Type) 匹配。
     *
     * 逻辑：它会先去 Spring 容器里找有没有 OllamaChatClient 这种类型的 Bean。
     *
     * 如果只找到 1 个：直接注入（成功）。
     *
     * 如果找到 0 个：报错（除非设置 required=false）。
     *
     * 如果找到多个：它会尝试根据变量名（chatClient）来区分；如果还区分不出来，就会报错。
     *
     * 来源：属于 Spring 框架 (org.springframework.beans.factory.annotation.Autowired)。
     *
     * @Resource (Java 标准)
     *
     * 默认行为：按名称 (By Name) 匹配。
     *
     * 逻辑：它会优先去找 ID（Bean Name）叫 chatClient 的 Bean。
     *
     * 如果按名字找不到，它才会回退去按类型找。
     *
     * 来源：属于 Java 标准规范 (JSR-250)，也就是 javax.annotation (或新的 jakarta.annotation)。
     */
    @Resource
    private OllamaChatClient chatClient;

    /**
     * 这个  @RequestMapping(value = "generate",method = RequestMethod.GET) 玩意儿就等于@GetMapping("/generate")
     * 调用参考http://localhost:8080/generate?mode=deepseek-r1:1.5b&message=你好
     * @param model
     * @param message
     * @return
     */
    @Override
    @RequestMapping(value = "generate",method = RequestMethod.GET)
    public ChatResponse generate(@RequestParam String model, @RequestParam String message) {
        return chatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
    }
    
    @RequestMapping(value = "generate_stream", method = RequestMethod.GET, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Override
    public Flux<ChatResponse> generateStream(@RequestParam String model, @RequestParam String message) {
        return chatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
    }
}
