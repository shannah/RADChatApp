
package com.codename1.cn1chat;

import com.codename1.capture.Capture;
import com.codename1.components.ToastBar;
import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.rad.controllers.Controller;
import com.codename1.rad.controllers.FormController;
import com.codename1.rad.models.Entity;
import com.codename1.rad.models.EntityList;
import com.codename1.rad.nodes.ActionNode;
import com.codename1.rad.nodes.ViewNode;
import com.codename1.rad.schemas.ChatMessage;
import com.codename1.rad.schemas.ChatRoom;
import com.codename1.rad.schemas.Person;
import com.codename1.rad.ui.UI;
import com.codename1.rad.ui.chatroom.ChatBubbleView;
import com.codename1.rad.ui.chatroom.ChatRoomView;
import static com.codename1.ui.CN.CENTER;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

// We're going to use a lot of static functions from the UI class for creating
// UI elements like actions declaratively, so we'll do a static import here.
import static com.codename1.rad.ui.UI.*;
import com.codename1.rad.ui.entityviews.ProfileAvatarView;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;


public class ChatFormController extends FormController {
    
    // Define the "SEND" action for the chat room
    public static final ActionNode send = action(
        enabledCondition(entity-> {
            return !entity.isEmpty(ChatRoom.inputBuffer);
        }),
        icon(FontImage.MATERIAL_SEND)
    );
    
    public static final ActionNode phone = action(
        icon(FontImage.MATERIAL_PHONE),
        condition(entity->{
            return CN.canDial() && !entity.isEmpty(Person.telephone);
        })
    );
    
    public static final ActionNode videoConference = action(
        icon(FontImage.MATERIAL_VIDEOCAM)
    );
    
    public static final ActionNode likedBadge = UI.action(
            UI.uiid("ChatBubbleLikedBadge"),
            icon(FontImage.MATERIAL_FAVORITE),
            condition(entity->{
                return !entity.isFalsey(ChatMessage.isFavorite);
            })
            
    );
    
    public static final ActionNode likeAction = UI.action(
            icon(FontImage.MATERIAL_FAVORITE_OUTLINE),
            uiid("LikeButton"),
            selected(icon(FontImage.MATERIAL_FAVORITE)),
            selectedCondition(entity->{
                return !entity.isFalsey(ChatMessage.isFavorite);
            })
            
    );
    
    public static final ActionNode capturePhoto = action(
            icon(FontImage.MATERIAL_CAMERA)
    );
    
    public ChatFormController(Controller parent) {
        super(parent);
        Form f = new Form("My First Chat Room", new BorderLayout());
        
        // Create a "view node" as a UI descriptor for the chat room.
        // This allows us to customize and extend the chat room.
        ViewNode viewNode = new ViewNode(
            actions(ChatRoomView.SEND_ACTION, send),
            actions(ProfileAvatarView.PROFILE_AVATAR_CLICKED_MENU, phone, videoConference),
            actions(ChatBubbleView.CHAT_BUBBLE_LONG_PRESS_MENU, likeAction),
            actions(ChatBubbleView.CHAT_BUBBLE_BADGES, likedBadge),
            actions(ChatRoomView.TEXT_ACTIONS, capturePhoto)
        );
        
        // Add the viewNode as the 2nd parameter
        ChatRoomView view = new ChatRoomView(createViewModel(), viewNode, f);
        f.add(CENTER, view);
        setView(f);
        
        addActionListener(send, evt->{
            evt.consume();
            ChatRoomView.ViewModel room = (ChatRoomView.ViewModel)evt.getEntity();
            String textFieldContents = room.getInputBuffer();
            if (textFieldContents != null && !textFieldContents.isEmpty()) {
                ChatBubbleView.ViewModel message = new ChatBubbleView.ViewModel();
                message.messageText(textFieldContents);
                message.date(new Date());
                message.isOwn(true); // Indicates that this is sent by "this" user 
                                    // so bubble is on right side of room view.
               
                // Now add the message
                room.addMessages(message);
                
                // Clear the text field contents
                room.inputBuffer("");
            }
            
        });
        
        addActionListener(phone, evt->{
            evt.consume();
            if (!CN.canDial()) {
                Dialog.show("Not supported", "Phone calls not supported on this device", "OK", null);
                return;
            }
            if (evt.getEntity().isEmpty(Person.telephone)) {
                Dialog.show("No Phone", "This user has no phone number", "OK", null);
                return;
            }
            
            String phoneNumber = evt.getEntity().getText(Person.telephone);
            CN.dial(phoneNumber);
              
        });
        
        addActionListener(likeAction, evt->{
            evt.consume();
            Entity chatMessage = evt.getEntity();
            chatMessage.setBoolean(
                    ChatMessage.isFavorite, 
                    chatMessage.isFalsey(ChatMessage.isFavorite)
            );
            
        });
        
        addActionListener(capturePhoto, evt->{
            evt.consume();
            String photoPath = Capture.capturePhoto();
            if (photoPath == null) {
                // User canceled the photo capture
                return;
            }
            
            
            File photos = new File("photos");
            photos.mkdirs();
            Entity entity = evt.getEntity();
            File photo = new File(photos, System.currentTimeMillis()+".png");
            try (InputStream input = FileSystemStorage.getInstance().openInputStream(photoPath);
                    OutputStream output = FileSystemStorage.getInstance().openOutputStream(photo.getAbsolutePath())) {
                Util.copy(input, output);
                
                ChatBubbleView.ViewModel message = new ChatBubbleView.ViewModel();
                message.attachmentImageUrl(photo.getAbsolutePath());
                message.isOwn(true);
                message.date(new Date());
                EntityList messages = entity.getEntityList(ChatRoom.messages);
                if (messages == null) {
                    throw new IllegalStateException("This chat room has no messages list set up");
                }
                messages.add(message);
                
            } catch (IOException ex) {
                Log.e(ex);
                ToastBar.showErrorMessage(ex.getMessage());
            }
        });
        
        
    }
    
    /**
     * Creates a view model for the chat room.
     * @return 
     */
    private Entity createViewModel() {
        ChatRoomView.ViewModel room = new ChatRoomView.ViewModel();
        long SECOND = 1000l;
        long MINUTE = SECOND * 60;
        long HOUR = MINUTE * 60;
        long DAY = HOUR * 24;
        long t = System.currentTimeMillis() - 2 * DAY;
    
        String georgeThumb = "https://weblite.ca/cn1tests/radchat/george.jpg";
        String kramerThumb = "https://weblite.ca/cn1tests/radchat/kramer.jpg";
        
        room.addMessages(createDemoMessage("Why couldn't you have made me an architect? You know I always wanted to pretend that I was an architect. "
                + "Well I'm supposed to see her tomorrow, I'm gonna tell her what's goin on. Maybe she likes me for me.",
                new Date(t), "George", georgeThumb));
        t += HOUR;
        room.addMessages(createDemoMessage("Hey", new Date(t), "Kramer", kramerThumb));
        t += MINUTE;
        room.addMessages(createDemoMessage("Hey", new Date(t), null,  null));
        
        room.addParticipants(
                new ChatAccount("George", georgeThumb, "712-555-1234"), 
                new ChatAccount("Kramer", kramerThumb, null)
        );
        return room;
    }
    
    private Entity createDemoMessage(String text, Date datePosted, String participant, String iconUrl) {
        ChatBubbleView.ViewModel msg = new ChatBubbleView.ViewModel();
        msg.messageText(text)
                .date(datePosted)
                .iconUrl(iconUrl)
                .isOwn(participant == null);
        if (participant != null) {
            msg.postedBy(participant);
        }
        return msg;
     
    }
    
}
