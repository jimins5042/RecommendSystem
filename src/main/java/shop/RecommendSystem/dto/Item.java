package shop.RecommendSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
@AllArgsConstructor
@Getter@Setter@Builder
public class Item {
    private Long itemId;
    private String itemTitle;
    private String itemContent;
    private Date createdAt;
    private Date updatedAt;
    private Long itemPrice;
    private String itemImageLink;
    private String hashCode;
    private byte[] bitArray;

    public Item() {

    }
}


