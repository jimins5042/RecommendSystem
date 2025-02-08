package shop.RecommendSystem.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import shop.RecommendSystem.dto.ImageInfo;
import shop.RecommendSystem.dto.Reply;
import shop.RecommendSystem.dto.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
CREATE TABLE reply_board
(
    reply_id       INT AUTO_INCREMENT PRIMARY KEY,
    post_id        INT NOT NULL,
    reply_writer   TEXT,
    reply_password TEXT,
    reply_content  TEXT NOT NULL,
    reply_date     DATETIME DEFAULT CURRENT_TIMESTAMP
);
 */
@Mapper
public interface ReplyMapper {
    ArrayList<Reply> selectReplyList(Long postId);

    void saveReply(Reply reply);
}
