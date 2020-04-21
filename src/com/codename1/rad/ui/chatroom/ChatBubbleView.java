/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.rad.ui.chatroom;

import ca.weblite.shared.components.ComponentImage;
import com.codename1.rad.ui.AbstractEntityView;
import com.codename1.rad.ui.Actions;
import com.codename1.rad.ui.DefaultEntityListCellRenderer;
import com.codename1.rad.ui.EntityView;
import com.codename1.rad.ui.image.AsyncImage;
import com.codename1.rad.ui.image.FirstCharEntityImageRenderer;
import com.codename1.rad.ui.menus.PopupActionsMenu;
import com.codename1.rad.nodes.ActionNode.Category;
import com.codename1.rad.nodes.ListNode;
import com.codename1.rad.nodes.Node;
import com.codename1.rad.nodes.ViewNode;
import ca.weblite.shared.components.OverflowContainer;
import com.codename1.components.ScaleImageLabel;
import com.codename1.components.SpanButton;
import com.codename1.rad.models.BooleanProperty;
import com.codename1.rad.models.DateProperty;
import com.codename1.rad.models.Entity;
import com.codename1.rad.models.EntityList;
import com.codename1.rad.models.EntityType;
import com.codename1.rad.models.Property;
import com.codename1.rad.models.StringProperty;
import com.codename1.rad.models.Tag;
import com.codename1.rad.schemas.ChatMessage;
import com.codename1.rad.schemas.Comment;
import com.codename1.rad.schemas.ListRowItem;
import com.codename1.rad.schemas.Thing;
import com.codename1.rad.text.LocalDateTimeShortStyleFormatter;
import com.codename1.io.Log;
import com.codename1.rad.attributes.UIID;
import com.codename1.rad.nodes.ActionNode;
import com.codename1.rad.ui.UI;
import com.codename1.rad.ui.animations.TypingAnimation;
import com.codename1.rad.ui.entityviews.ProfileAvatarView;
import com.codename1.rad.ui.entityviews.EntityListView;
import com.codename1.rad.ui.image.EntityImageRenderer;
import com.codename1.ui.CN;
import static com.codename1.ui.CN.NORTH;
import static com.codename1.ui.CN.WEST;
import com.codename1.ui.Component;
import static com.codename1.ui.ComponentSelector.$;
import com.codename1.ui.Container;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.URLImage;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.RoundRectBorder;
import java.util.Date;
import java.util.Objects;

/**
 * A view for a single row/chat bubble in a Chat view.  This view is used to render rows for the {@link ChatRoomView}
 * view.
 * 
 * <h3>View Model Requirements</h3>
 * 
 * <p>See {@link ViewModel} for a reference view model for use in this view.  However, you don't need to use this class.  Any 
 * entity that implements the required properties will do.</p>
 * 
 * <p><strong>Properties:</strong></p>
 * 
 * <ul>
 *  <li><strong>{@link ChatMessage#text}</strong> - Required.  The text to appear in the chat bubble.</li>
 *  <li><strong>{@link ChatMessage#creator}</strong> - Optional.  The name of the person who sent the chat message.</li>
 *  <li><strong>{@link ChatMessage#thumbnailUrl}</strong> or {@link ListRowItem#icon} - Optional.  The icon or URL to 
 *     the user's avatar who posted the message.
 *  </li>
 *  <li><strong>{@link ChatMessage#datePublished}</strong> - Optional. The date-time the message was posted.</li>
 *  <li><strong>{@link ChatMessage#isOwnMessage}</strong> - Optional.  Boolean flag indicating that the message was posted by the current user.  
 *      If this isn't present, it will use an empty {@link ChatMessage#creator} field to imply "true" on this field.
 * </li>
 * 
 * </ul>
 * 
 * <h3>Supported Actions</h3>
 * 
 * <p>This view supports the following actions:</p>
 * 
 * <ul>
 *  <li>{@link #CHAT_BUBBLE_CLICKED}</li>
 *  <li>{@link #CHAT_BUBBLE_LONG_PRESS}</li>
 *  <li>{@link #CHAT_BUBBLE_CLICKED_MENU}</li>
 *  <li>{@link #CHAT_BUBBLE_LONG_PRESS_MENU}</li>
 *  <li>{@link #CHAT_BUBBLE_BADGES}</li>
 * </ul>
 * 
 * <p>In addition to these actions, the poster's "icon" or thumbnail URL is rendered by the {@link ProfileAvatarView} view
 * which supports several actions as well.  Actions defined on this chat bubble's view node will be accessible to the 
 * embedded {@link ProfileAvatarView}.</p>
 * 
 * 
 * 
 * 
 * @author shannah
 */
public class ChatBubbleView<T extends Entity> extends AbstractEntityView<T> {
    
    /**
     * Actions displayed in popup menu when user long presses a chat bubble.
     */
    public static final Category CHAT_BUBBLE_LONG_PRESS_MENU = new Category();
    
    /**
     * Actions displayed in popup menu when user clicks chat bubble.
     */
    public static final Category CHAT_BUBBLE_CLICKED_MENU = new Category();
    
    /**
     * Action fired when user clicks chat bubble.
     */
    public static final Category CHAT_BUBBLE_CLICKED = new Category();
    
    /**
     * Action fired when user longpresses chat bubble.
     */
    public static final Category CHAT_BUBBLE_LONG_PRESS = new Category();
    
    /**
     * Actions displayed as "badges" of a chat bubble.
     */
    public static final Category CHAT_BUBBLE_BADGES = new Category();
    
    
    private boolean hideDate;
    private Node viewNode;
    public static final Tag TEXT = ChatMessage.text;
    public static final Tag icon = ListRowItem.icon;
    private SpanButton text = new SpanButton();
    private Label date = new Label(), iconLabel = new Label(), postedByLabel = new Label();
    private ProfileAvatarView avatar;
    private Property textProp, postedByProp, iconProp, dateProp, isOwnProp, typingInProgress, attachment, attachmentPlaceholder;
    private RoundRectBorder bubbleBorder;
    private boolean isOwnMessage;
    private Container wrapper = new Container(new BorderLayout());
    
    /**
     * Creates a new chat bubble view for the given entity.
     * @param entity The view model.  See {@link ViewModel} for a reference view model, but custom classes will work fine.  The view expects properties with tags {@link ChatMessage#text},
     * {@link ListRowItem#icon} (or {@link ChatMessage#thumbnailUrl}, {@link ChatMessage#datePublished}, {@link ChatMessage#isOwnMessage}, {@link ChatMessage#typingInProgress},
     * and {@link ChatMessage#creator}.  However, if a property is missing, it will generally fall-back sensibly, just omitting that information.  The only critical
     * property is {@link ChatMessage#text} which is used as the text of the chat bubble.
     * @param viewNode The view descriptor. Used to pass view parameters, actions, and other view settings.
     */
    public ChatBubbleView(T entity, Node viewNode) {
        super(entity);
        this.viewNode = viewNode;
        setLayout(new BorderLayout());
        textProp = entity.findProperty(TEXT, Thing.description);
        if (textProp == null) {
            throw new IllegalArgumentException("Cannot create chat bubble for entity because it doesn't have any properties tagged with TEXT or Thing.description.");
        }
        postedByProp = entity.findProperty(Comment.creator);
        
        iconProp = entity.findProperty(icon, Comment.thumbnailUrl);
        
        dateProp = entity.findProperty(Comment.datePublished, Comment.dateCreated, Comment.dateModified);
        typingInProgress = entity.findProperty(ChatMessage.typingInProgress);
        isOwnProp = entity.findProperty(ChatMessage.isOwnMessage);
        attachment = entity.findProperty(ChatMessage.attachment);
        attachmentPlaceholder = entity.findProperty(ChatMessage.attachmentPlaceholderImage);
        postedByLabel.setUIID("ChatBubblePostedBy");
        date.setUIID("ChatBubbleDate");
        text.setIconPosition(BorderLayout.SOUTH);
        text.addActionListener(evt->{
            evt.consume();
            ActionNode action = viewNode.getInheritedAction(CHAT_BUBBLE_CLICKED);
            if (action != null) {
                ActionEvent ae = action.fireEvent(entity, ChatBubbleView.this);
                if (ae.isConsumed()) {
                    return;
                }
            }
            
            Actions menu = viewNode.getInheritedActions(CHAT_BUBBLE_CLICKED_MENU).getEnabled(entity);
            if (!menu.isEmpty()) {
                PopupActionsMenu p = new PopupActionsMenu(menu, entity, text);
                p.setCommandsLayout(new GridLayout(1, menu.size()));
                p.showPopupDialog(text);
                return;
                
            }
        });
        
        text.addLongPressListener(evt->{
            evt.consume();
            ActionNode action = viewNode.getInheritedAction(CHAT_BUBBLE_LONG_PRESS);
            if (action != null) {
                ActionEvent ae = action.fireEvent(entity, ChatBubbleView.this);
                if (ae.isConsumed()) {
                    return;
                }
            }
            
            Actions menu = viewNode.getInheritedActions(CHAT_BUBBLE_LONG_PRESS_MENU).getEnabled(entity);
            if (!menu.isEmpty()) {
                PopupActionsMenu p = new PopupActionsMenu(menu, entity, text);
                p.setCommandsLayout(new GridLayout(1, menu.size()));
                p.showPopupDialog(text);
                return;
                
            }
        });
        
        update();
    }


    
    @Override
    protected void initComponent() {
        super.initComponent();
        
    }

    @Override
    protected void deinitialize() {

        super.deinitialize();
    }

    
    private boolean lastTypingInProgress;
    private String lastAttachmentPlaceholderImageURL;
    
    
    private ComponentImage createPlaceholderImage(int width, int height) {
        Label placeholder = new Label();
        $(placeholder).selectAllStyles()
                .setBorder(Border.createEmpty())
                .setBgTransparency(0x0)
                .setFgColor(0x999999)
                ;

        placeholder.setMaterialIcon(com.codename1.ui.FontImage.MATERIAL_IMAGE, 10);

        Container placeholderCnt = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
        $(placeholderCnt)
                .setBgTransparency(0x0)
                .setFgColor(0x999999)
                .setBorder(RoundRectBorder.create().cornerRadius(1f).strokeColor(0x666666).stroke(25.4f/96f/2f, true))

                ;
        placeholderCnt.addComponent(BorderLayout.CENTER, placeholder);
        placeholderCnt.setWidth(width);
        placeholderCnt.setHeight(height);
        placeholderCnt.layoutContainer();
        return new ComponentImage(placeholderCnt, placeholderCnt.getWidth(), placeholderCnt.getHeight());
    }
    
    @Override
    public void update() {
        boolean changed = false;
        
        if (typingInProgress != null && getEntity().getBoolean(typingInProgress) != lastTypingInProgress) {
            lastTypingInProgress = getEntity().getBoolean(typingInProgress);
            changed = true;
        }
        if (!getEntity().isFalsey(attachmentPlaceholder) && !Objects.equals(getEntity().getText(attachmentPlaceholder), lastAttachmentPlaceholderImageURL)) {
            int imSize = Math.min(CN.getDisplayWidth() * 7 / 8 , CN.getDisplayHeight() * 7 / 8);
            imSize = Math.min(imSize, CN.convertToPixels(35));
            Image im = getEntity().createImageToStorage(
                    attachmentPlaceholder, 
                    createPlaceholderImage(imSize, imSize).toEncodedImage(), 
                    null, URLImage.RESIZE_SCALE_TO_FILL);
            text.setIcon(im);
            lastAttachmentPlaceholderImageURL = getEntity().getText(attachmentPlaceholder);
            changed = true;
        } else if (lastAttachmentPlaceholderImageURL != null && !lastAttachmentPlaceholderImageURL.isEmpty() && getEntity().isEmpty(attachmentPlaceholder)) {
            text.setIcon(null);
            lastAttachmentPlaceholderImageURL = null;
            changed = true;
        }
        
        String newText = getEntity().getEntityType().getText(textProp, getEntity());
        if (!Objects.equals(newText, text.getText())) {
            text.setText(newText);
            changed = true;
        }
        
        if (postedByProp != null) {
            Object postedByObj = getEntity().get(postedByProp);
            String postedByStr = "";
            if (postedByObj instanceof Entity) {
                Entity postedByEntity = (Entity)postedByObj;
                postedByStr = postedByEntity.getEntityType().getText(postedByEntity, Thing.name, Thing.alternateName, Thing.identifier);
                
            } else {
                postedByStr = postedByObj == null ? "" : String.valueOf(postedByObj);
            }
            if (postedByStr != null && !Objects.equals(postedByStr, postedByLabel.getText())) {
                postedByLabel.setText(postedByStr);
                
                changed = true;
            }
        }
        
        if (dateProp != null) {
            //String newDate = getEntity().getEntityType().getText(dateProp, getEntity());
            Object newDate = getEntity().get(dateProp);
            //System.out.println("new date is "+newDate+" class="+newDate.getClass());
            if (newDate instanceof Date) {
                
                LocalDateTimeShortStyleFormatter formatter = new LocalDateTimeShortStyleFormatter();
                String dateString = formatter.format((Date)newDate);
                if (!Objects.equals(dateString, date.getText())) {
                    date.setText(dateString);
                    if (hideDate) {
                        date.setHidden(true);
                    }
                    changed = true;
                }
            }
        }
        
        Object iconVal = iconProp == null ? null : iconProp.getValue(getEntity());
        float sizeMM = 5;
        if (CN.isDesktop()) {
            sizeMM *= 2;
        }
        if (iconVal != null && avatar == null) {
            
            /*
            AsyncImage icon = (AsyncImage)getEntity().get(iconProp, AsyncImage.CONTENT_TYPE);
            if (icon != null) {
                icon.ready(im->{
                    iconLabel.setIcon(im);
                    //revalidateWithAnimationSafety();
                });
            }
            */
            
            ViewNode avatarViewNode = new ViewNode(
                    UI.param(ProfileAvatarView.ICON_PROPERTY, iconProp),
                    UI.param(ProfileAvatarView.NAME_PROPERTY, postedByProp)
            );
            avatarViewNode.setParent(getViewNode());
            avatar = new ProfileAvatarView(
                    getEntity(),
                    avatarViewNode,
                    sizeMM
            );
        } else if (iconVal == null && avatar == null && postedByProp != null) {
            //FirstCharEntityImageRenderer renderer = new FirstCharEntityImageRenderer(5);
            if (postedByProp.getContentType().isEntity()) {
                
                ViewNode avatarNode = new ViewNode();
                avatarNode.setParent(getViewNode());
                avatar = new ProfileAvatarView(
                        (Entity)getEntity().get(postedByProp),
                        avatarNode,
                        sizeMM
                );

            } else {
                
                
                
                EntityImageRenderer renderer = new FirstCharEntityImageRenderer(sizeMM);
                renderer.createImage(this, postedByProp, 0, false, false).ready(im->{
                    iconLabel.setIcon(im);
                    //revalidateWithAnimationSafety();
                });
            }/*
            EntityImageRenderer renderer = postedByProp.getContentType().isEntity() ? 
                    new ProfileAvatarView.ImageRenderer(sizeMM) : new FirstCharEntityImageRenderer(sizeMM);
            renderer.createImage(this, postedByProp, 0, false, false).ready(im->{
                iconLabel.setIcon(im);
                //revalidateWithAnimationSafety();
            });
            */
        }
        
        if (changed) {
            
            wrapper.removeAll();
            wrapper.remove();
            removeAll();
            //wrapper.setLayout(new BorderLayout());
            
            if (isOwnProp != null) {
                isOwnMessage = getEntity().getBoolean(isOwnProp);
            } else {
                isOwnMessage = postedByLabel.getText() == null || postedByLabel.getText().length() == 0;
            }
            
            if (date.getText() != null && date.getText().length() > 0) {
                date.remove();
                wrapper.add(NORTH, FlowLayout.encloseCenter(date));
            }
            Container center = new Container(BoxLayout.y());
            String uuidSuffix = CN.isDesktop() ? "Desktop" : "";
            if (isOwnMessage) {
                $(wrapper).setPaddingMillimeters(1, 1, 1, 10);
                text.remove();
                RoundRectBorder border = RoundRectBorder.create();
                border.cornerRadius(2f);
                border.shadowX(1);
                border.shadowY(1);
                border.setArrowSize(1.5f);
                border.trackComponentSide(RIGHT);
                border.trackComponentVerticalPosition(1f);
                bubbleBorder = border;
                
                //
                text.setUIID("ChatBubbleSpanLabelOwn");
                text.setTextUIID("ChatBubbleTextOwn" + uuidSuffix);
                if (text.getStyle().getBorder() == null || text.getStyle().getBorder().isEmptyBorder()) {
                    $(text).selectAllStyles().setBorder(border);
                }
                
                Component right = text;
                Actions badges = viewNode.getInheritedActions(CHAT_BUBBLE_BADGES);
                
                
                if (!badges.isEmpty()) {
                    Actions styledBadges = new Actions();
                    for (ActionNode a : badges) {
                        UIID u = a.getUIID();
                        if (u == null) {
                            u = new UIID("ChatBubbleBadge");
                            ActionNode styled = (ActionNode)a.createProxy(viewNode);
                            styled.setAttributes(u);
                            styledBadges.add(styled);
                        } else {
                            styledBadges.add(a);
                        }
                    }
                    Container badgesCnt = new Container(new GridLayout(badges.size(), 1));
                    styledBadges.addToContainer(badgesCnt, getEntity());
                    Container rightCnt = new Container(new LayeredLayout());
                    rightCnt.addComponent(text);
                    rightCnt.addComponent(badgesCnt);
                    LayeredLayout ll = (LayeredLayout)rightCnt.getLayout();
                    ll.setInsets(badgesCnt, "0 auto auto 0");
                    $(text).selectAllStyles().setMarginMillimeters(0, 0, 0, 3);
                    right = rightCnt;
                    
                }
                
                center.add(FlowLayout.encloseRight(right));
            } else {
                $(wrapper).selectAllStyles().setPaddingMillimeters(1, 10, 1, 1);
                if (postedByLabel.getText() != null && postedByLabel.getText().length() > 0) {
                    postedByLabel.remove();
                    center.add(FlowLayout.encloseIn(postedByLabel));
                }
                RoundRectBorder border = RoundRectBorder.create();
                border.cornerRadius(2f);
                border.shadowX(1);
                border.shadowY(1);
                border.setArrowSize(1.5f);
                border.trackComponentSide(LEFT);
                border.trackComponentVerticalPosition(1f);
                bubbleBorder = border;
                text.setUIID("ChatBubbleSpanLabelOther");
                text.setTextUIID("ChatBubbleTextOther"+uuidSuffix);
                if (text.getStyle().getBorder() == null || text.getStyle().getBorder().isEmptyBorder()) {
                    $(text).selectAllStyles().setBorder(border);
                }
                
                Component right = text;
                Actions badges = viewNode.getInheritedActions(CHAT_BUBBLE_BADGES);
                
                if (!badges.isEmpty()) {
                    
                    Actions styledBadges = new Actions();
                    for (ActionNode a : badges) {
                        UIID u = a.getUIID();
                        if (u == null) {
                            u = new UIID("ChatBubbleBadge");
                            ActionNode styled = (ActionNode)a.createProxy(viewNode);
                            styled.setAttributes(u);
                            styledBadges.add(styled);
                        } else {
                            styledBadges.add(a);
                        }
                    }
                    Container badgesCnt = new Container(new GridLayout(badges.size(), 1));
                    styledBadges.addToContainer(badgesCnt, getEntity());
                    Container rightCnt = new Container(new LayeredLayout());
                    rightCnt.addComponent(text);
                    rightCnt.addComponent(badgesCnt);
                    LayeredLayout ll = (LayeredLayout)rightCnt.getLayout();
                    ll.setInsets(badgesCnt, "0 0 auto auto");
                    $(text).selectAllStyles().setMarginMillimeters(0, 3, 0, 0);
                    right = rightCnt;
                    
                }
                
                center.add(FlowLayout.encloseIn(right));
                
                if (typingInProgress != null && getEntity().getBoolean(typingInProgress)) {
                    text.setIcon(new TypingAnimation().toImage());
                }
                
                wrapper.add(WEST, BoxLayout.encloseYBottom(avatar == null ? iconLabel : avatar));
            }
            wrapper.add(CENTER, center);
            if (date.getText() != null && date.getText().length() > 0) {
                Label date2 = new Label(date.getText());
                date2.setUIID("ChatBubbleDate");
                OverflowContainer overflowContainer = new OverflowContainer(wrapper, date2) {
                    @Override
                    protected void initComponent() {
                        super.initComponent();
                        OverflowContainer.OverflowGroup group = OverflowContainer.OverflowGroup.findGroup(this);
                        if (group == null) {
                            EntityListView parentList = findParentList(this);
                            if (parentList != null) {
                                group = OverflowContainer.OverflowGroup.createGroup(parentList);
                            }
                        }
                        if (group != null) {
                            group.add(this);
                        }
                    }

                    @Override
                    protected void deinitialize() {
                        OverflowContainer.OverflowGroup group = OverflowContainer.OverflowGroup.findGroup(this);
                        if (group != null) {
                            group.remove(this);
                        }

                        super.deinitialize();
                    }



                };
                add(CENTER, overflowContainer);
            } else {
                add(CENTER, wrapper);
            }
            
            
        }
        
        
        
    }
    
    private static EntityListView findParentList(Component searchStart) {
        while (searchStart != null) {
            if (searchStart instanceof EntityListView) {
                return (EntityListView)searchStart;
            }
            searchStart = searchStart.getParent();
        }
        return null;
    }

    @Override
    public void commit() {
        
    }

    @Override
    public Node getViewNode() {
        return viewNode;
    }
    
    /**
     * A list cell renderer for creating a row of {@link ChatBubbleView} inside an {@link EntityListView}.
     */
    public static class ChatBubbleListCellRenderer extends DefaultEntityListCellRenderer {
        //private DateUtil dateUtil = new DateUtil();
        @Override
        public EntityView getListCellRendererComponent(EntityListView list, Entity value, int index, boolean isSelected, boolean isFocused) {
            ListNode listNode = (ListNode)list.getViewNode();
            EntityList listEntity = (EntityList)list.getEntity();
            
            
            try {
                ChatBubbleView v = new ChatBubbleView(value, listNode.getRowTemplate());
                if (index > 0 && v.dateProp != null) {
                    Entity prev = listEntity.get(index-1);
                    Object prevDate = prev.get(v.dateProp);
                    Object currDate = value.get(v.dateProp);
                    if (prevDate instanceof Date && currDate instanceof Date) {
                        Date d1 = (Date)prevDate;
                        Date d2 = (Date)currDate;
                        if (Math.abs(d1.getTime() - d2.getTime()) < 1000 * 60 * 30l) {
                            v.hideDate = true;
                            v.date.setHidden(true);
                        }
                    }

                }
                return v;
            } catch (Throwable t) {
                Log.e(t);
                return super.getListCellRendererComponent(list, value, index, isSelected, isFocused);
            }
        }
        
    }
    
    /*
    private ActionListener longPressListener = evt -> {
        if (text.contains(evt.getX(), evt.getY())) {
            
            ViewNode node = (ViewNode)getViewNode();
            Actions chatActions = node.getInheritedActions(CHAT_BUBBLE_LONG_PRESS_MENU);
            if (!chatActions.isEmpty()) {
                PopupActionsMenu menu = new PopupActionsMenu(chatActions, getEntity(), this);
                menu.showPopupDialog(text);
            }
            return;
        }
        if (iconLabel.contains(evt.getX(), evt.getY())) {
            ViewNode node = (ViewNode)getViewNode();
            Actions chatActions = node.getInheritedActions(CHAT_SENDER_ACTIONS);
            if (!chatActions.isEmpty()) {
                PopupActionsMenu menu = new PopupActionsMenu(chatActions, getEntity(), this);
                menu.showPopupDialog(text);
            }
            return;
        }
        
    };
    */
    
    /**
     * This is a view model class that *may* be used as a model for the ChatBubbleView, as it 
     * defines properties with all of the required tags.  You can also use your own entity
     * for a view model as long as the entity contains properties with the given tags.  E.g.
     * You can use any entity as long as it includes at least a property with the {@link #messageTextTag} tag, which
     * is the same as {@link Comment#text}.
     * 
     * <p>Note: If you use a custom view model, you needn't use the same content types for your properties.  E.g.
     * this sample view model's iconTag is a string URL.  But you could just as easily use an image property,
     * and the view will handle it properly.
     * 
     */
    public static class ViewModel extends Entity {
        
        /**
         * Tag used for the date field.
         */
        public static final Tag dateTag = Comment.datePublished;
        
        /**
         * Tag used for the "own" flag of a chat message to indicate that
         * the message originated from the current user, and not another user.  This flag
         * is used to determine which side (left or right) of the view the chat bubble
         * is displayed on.
         * 
         * <p>If the view model entity does not contain a property with this tag, then it will
         * be inferred by the ChatBubbleView based on whether the "postedBy" property is empty.</p>
         * 
         * @see ChatMessage#isOwnMessage
         */
        public static final Tag isOwnTag = ChatMessage.isOwnMessage;
        
        /**
         * Tag used for boolean property which marks a chat message as a "favourite".
         * @see ChatMessage#isFavorite
         */
        public static final Tag isFavorite = ChatMessage.isFavorite;
        
        /**
         * Tag used for the "icon" property of a chat message.  The field may be any content
         * type that can be converted to an {@link AsyncImage}.  This includes a string (with a URL,
         * fiile path, resource path, or storage key), or any other content type that can convert
         * to AsyncImage.
         * @see Comment#thumbnailUrl
         */
        public static final Tag iconTag = Comment.thumbnailUrl;
        
        /**
         * Tag used for the 'posted by' property of a chat message.  This reference implementation
         * uses a String property for this, but you could just as well use an Entity content type
         * to store an actual entity of a user.
         * @see Comment#creator
         */
        public static final Tag postedByTag = Comment.creator;
        
        /**
         * Tag used for the message text property of a chat message.  This is the only *required*
         * tag for an entity to be usable as a model for a ChatBubbleView.
         * 
         * @see Comment#text
         */
        public static final Tag messageTextTag = Comment.text;
        
        /**
         * Tag used for boolean property that indicates typing is still in progress for a chat message.
         * 
         * @see ChatMessage#typingInProgress
         */
        public static final Tag typingInProgressTag = ChatMessage.typingInProgress;
        
        public static final Tag attachmentPlaceholder = ChatMessage.attachmentPlaceholderImage;
        
        public static StringProperty postedBy, iconUrl, messageText, attachmentImageUrl;
        public static BooleanProperty own, favorite, typingInProgress;
        public static DateProperty date;
        private static final EntityType TYPE = new EntityType(){{
            postedBy = string(tags(postedByTag));
            own = Boolean(tags(isOwnTag));
            iconUrl = string(tags(iconTag));
            date = date(tags(dateTag));
            messageText = string(tags(messageTextTag));
            favorite = Boolean(tags(isFavorite));
            typingInProgress = Boolean(tags(typingInProgressTag));
            attachmentImageUrl = string(tags(attachmentPlaceholder));
        }};
        
        {
            setEntityType(TYPE);
            isFavorite(false);
            isOwn(true);
            typingInProgress(false);
        }
        
        public ViewModel postedBy(String username) {
            set(postedBy, username);
            isOwn(username == null || username.length() == 0);
            return this;
        }
        
        public ViewModel iconUrl(String url) {
            set(iconUrl, url);
            return this;
        }
        
        public ViewModel date(Date d) {
            set(date, d);
            return this;
        }
        
        public ViewModel messageText(String text) {
            set(messageText, text);
            return this;
        }
        
        public ViewModel isOwn(boolean o) {
            set(own, o);
            return this;
        }
        
        public ViewModel isFavorite(boolean o) {
            set(favorite, o);
            return this;
        }
        
        public Boolean isTypingInProgress() {
            return get(typingInProgress);
        }
        
        public ViewModel typingInProgress(boolean inProgress) {
            set(typingInProgress, inProgress);
            return this;
        }
        
        public ViewModel attachmentImageUrl(String imageUrl) {
            set(attachmentImageUrl, imageUrl);
            return this;
        }
        
        public String getPostedBy() {
            return get(postedBy);
        }
        
        public String getIconUrl() {
            return get(iconUrl);
        }
        
        public Date getDate() {
            return get(date);
        }
        
        public Boolean isOwn() {
            return get(own);
        }
        
        public Boolean isFavorite() {
            return get(favorite);
        }
        
        public String getMessageText() {
            return get(messageText);
        }
        
        public String getAttachmentImageUrl() {
            return get(attachmentImageUrl);
        }
    }
    
}
