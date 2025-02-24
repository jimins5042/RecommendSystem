package shop.RecommendSystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter@Setter
public class Item {
    private Long itemId;
    private String itemTitle;
    private String itemContent;
    private Date itemDate;
    private Long itemPrice;
    private String itemImageLink;
    private String hashCode;
    private byte[] bitArray;

    public Item() {

    }

    public Item(String itemTitle, String itemContent, Long itemPrice, String itemImageLink) {

        this.itemTitle = itemTitle;
        this.itemContent = itemContent;
        this.itemPrice = itemPrice;
        this.itemImageLink = itemImageLink;
    }

    public Item(Long itemId, String itemTitle, String itemContent, Date itemDate, Long itemPrice, String itemImageLink, String hashCode) {
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.itemContent = itemContent;
        this.itemDate = itemDate;
        this.itemPrice = itemPrice;
        this.itemImageLink = itemImageLink;
        this.hashCode = hashCode;
    }

    public Item(Long itemId, String itemTitle, String itemContent, Date itemDate, Long itemPrice, String itemImageLink, String hashCode, byte[] bitArray) {
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.itemContent = itemContent;
        this.itemDate = itemDate;
        this.itemPrice = itemPrice;
        this.itemImageLink = itemImageLink;
        this.hashCode = hashCode;
        this.bitArray = bitArray;
    }
}


