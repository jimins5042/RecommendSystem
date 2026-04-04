package shop.RecommendSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Getter@Setter@Builder
public class Item {

    private Long itemId;
    private String itemTitle;
    private String itemContent;
    private Long itemPrice;
    private Date createdAt;
    private Date updatedAt;
    private String category;
    private List<String> imageUrlList;
    private String imageUrl;
    private String imageLink;
    private String itemImageLink;
    private String hashCode;
    private byte[] bitArray;
    public Item() {

    }
}


