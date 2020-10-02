/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.cn1chat;

import com.codename1.rad.models.Entity;
import com.codename1.rad.models.EntityType;
import com.codename1.rad.schemas.ChatMessage;

/**
 *
 * @author shannah
 */
public class ChatMessageModel extends Entity {
    public static final EntityType TYPE = new EntityType(){{
        string(ChatMessage.text);
        date(ChatMessage.datePublished);
        entity(ChatAccount.class, ChatMessage.creator);
        Boolean(ChatMessage.isOwnMessage);
        Boolean(ChatMessage.isFavorite);
        string(ChatMessage.attachmentPlaceholderImage);
    }};
    {
        setEntityType(TYPE);
        
    }
    
}
