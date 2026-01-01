package cn.bugstack.xfg.dev.tech.api;

import org.springframework.ai.chat.ChatResponse;
import reactor.core.publisher.Flux;

public interface IAiService {
    ChatResponse generate(String mode,String message);

    Flux<ChatResponse> generateStream(String mode, String message);

}
