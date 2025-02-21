package shop.RecommendSystem.shoppingMall;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import shop.RecommendSystem.dto.Reply;
import shop.RecommendSystem.repository.mapper.ReplyMapper;

import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReplyService {

    private final ReplyMapper replyMapper;

    public ArrayList<Reply> getReplies(long id) {
        return replyMapper.selectReplyList(id);
    }

    public void saveReply(Reply reply) {
        replyMapper.saveReply(reply);
    }
}
