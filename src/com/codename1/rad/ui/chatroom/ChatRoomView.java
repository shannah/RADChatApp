/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.rad.ui.chatroom;

import com.codename1.rad.ui.AbstractEntityView;
import com.codename1.rad.ui.ActionStyle;
import com.codename1.rad.ui.Actions;
import com.codename1.rad.ui.EntityEditor;
import com.codename1.rad.ui.EntityView;
import com.codename1.rad.ui.EntityViewFactory;
import com.codename1.rad.ui.NodeList;
import com.codename1.rad.ui.UI;
import com.codename1.rad.nodes.ActionNode;
import com.codename1.rad.nodes.ActionNode.Category;
import com.codename1.rad.nodes.FieldNode;
import com.codename1.rad.nodes.ListNode;
import com.codename1.rad.nodes.Node;
import com.codename1.rad.nodes.ViewNode;
import com.codename1.rad.propertyviews.TextAreaPropertyView;
import com.codename1.rad.models.Entity;
import com.codename1.rad.models.EntityList;
import com.codename1.rad.models.EntityType;
import com.codename1.rad.models.ListProperty;
import com.codename1.rad.models.Property;
import com.codename1.rad.models.StringProperty;
import com.codename1.rad.models.Tag;
import com.codename1.rad.models.Tags;
import com.codename1.rad.schemas.ChatRoom;
import com.codename1.rad.schemas.Comment;
import com.codename1.rad.ui.entityviews.ProfileAvatarView;
import com.codename1.rad.ui.entityviews.ProfileAvatarsTitleComponent;
import com.codename1.rad.ui.entityviews.EntityListView;
import com.codename1.ui.CN;
import static com.codename1.ui.CN.SOUTH;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * A view that provides a fully-functional user interface for a multi-user chat.
 * @author shannah
 */
public class ChatRoomView<T extends Entity> extends AbstractEntityView<T> {
    
    public static final Category TEXT_ACTIONS = new Category();
    private Node node;
    private ListNode listNode;
    private Property messagesProp, participantsProp, textBufferProp;
    private EntityList messagesEntity;
    private Container wrapper = new Container(new BorderLayout());
    private TextArea entryField = new TextField() {
        @Override
        public int getBottomGap() {
            return 0;
        }
        
    };
    private TextAreaPropertyView entryFieldBinding;
    
    private Form form;
    
    /**
     * Category used to register an action to be fired when the user hits "send".
     */
    public static final Category SEND_ACTION = new Category();
    
    /**
     * Category used to register actions that the user can perform next to the 
     * text input field.  This is an appropriate place to add capabilities like
     * taking a photo.
     */
    //public static final Category TEXT_ACTIONS = new Category();
    
    /**
     * Creates a chat room with the settings specified by the given node.  This will create
     * a new view model of type {@link ViewModel}.
     * @param node UI node descriptor.  This node can be used to add actions to the chat room.
     */
    public ChatRoomView(Node node, Form form) {
        this((T)new ViewModel(), node, form);
    }
    
    /**
     * Creates a chat room view with a default view model of type {@link ViewModel}, and an empty
     * Node with no actions.
     */
    public ChatRoomView(Form form) {
        this((T)new ViewModel(), form);
    }
    
    /**
     * Creates a chat room view with the provided entity as the view model.  The view model can be any entity
     * with properties tagged with {@link ViewModel#messagesTag} and {@link ViewModel#inputBufferTag}.  See
     * {@link ViewModel} for a reference implementation and instructions on using custom entities
     * for a view model.
     * @param entity 
     */
    public ChatRoomView(T entity, Form form) {
        this(entity, createNode(), form);
    }
    
    private static Node createNode() {
        UI ui = new UI() {{
            form(
                view(
                    viewFactory(new ChatRoomView.ChatRoomViewFactory()),
                    UI.param(EntityListView.SCROLLABLE_Y, true)
                )
            );
        }};
        return ui.getRoot().getRootView();
    }
    private int actualRowsInTextEditor;
    
    /**
     * Creates a new ChatRoomView with the given view model (entity) and node description.
     * @param entity The entity to use as the view model. The view model can be any entity
     * with properties tagged with {@link ViewModel#messagesTag} and {@link ViewModel#inputBufferTag}.  See
     * {@link ViewModel} for a reference implementation and instructions on using custom entities
     * for a view model.
     * @param node The Node used as the UI descriptor for the chat room.  Set things like actions on this node.
     */
    public ChatRoomView(T entity, Node node, Form form) {
        super(entity);
        if (node == null) {
            node = createNode();
        }
        this.node = node;
        if (node.getViewParameter(EntityListView.SCROLLABLE_Y) == null) {
            node.setAttributes(UI.param(EntityListView.SCROLLABLE_Y, true));
        }
        setLayout(new BorderLayout());
        EntityType et = getEntity().getEntityType();
        
        // Need to find the field corresponding to the input buffer in the UI.
        NodeList childFields = node.getChildFieldNodes(new Tags(ChatRoom.inputBuffer));
        FieldNode fn = null;
        if (childFields.isEmpty()) {
            // No field definition found in the Node.  Add  FieldNode with
            // the correct configuration.
            fn = new FieldNode(UI.tags(ChatRoom.inputBuffer));
            childFields.add(fn);
            node.setAttributes(fn);
        } else {
            fn = (FieldNode)childFields.iterator().next();
        } 
        textBufferProp = fn.getProperty(et);
        if (textBufferProp == null) {
            throw new IllegalArgumentException("ChatRoomView view model requires a property with tag ChatRoom.inputBuffer that is missing");
        }
        entryField.setScrollVisible(false);
        entryField.setGrowByContent(true);
        entryField.setRows(1);
        actualRowsInTextEditor = entryField.getActualRows();
        entryField.setSingleLineTextArea(false);
        entryField.setMaxSize(99999999);
        String singleRowUiid = "ChatMessageTextArea";
        
        String uuidSuffix = CN.isDesktop() ? "Desktop" : "";
        String multilineSuffix = "MultiLine";
        entryField.setUIID("ChatMessageTextArea" + uuidSuffix);
        entryFieldBinding = new TextAreaPropertyView(entryField, getEntity(), fn);
        
        entryField.addDataChangedListener((o,i)->{
            if (actualRowsInTextEditor != entryField.getActualRows()) {
                actualRowsInTextEditor = entryField.getActualRows();
                if (actualRowsInTextEditor == 1) {
                    entryField.setUIID(singleRowUiid + uuidSuffix);
                } else {
                    System.out.println("UIID now "+singleRowUiid + uuidSuffix + multilineSuffix);
                    entryField.setUIID(singleRowUiid + uuidSuffix + multilineSuffix);
                }
                Form f = getComponentForm();
                if (f != null) {
                    f.revalidateWithAnimationSafety();
                }
            }
        });
        ActionNode te = node.getAction(SEND_ACTION);
        if (CN.isDesktop()) {
            // We only set a done listener on the desktop because on mobile there is no
            // way to properly have an enter button in the VKB AND have a submit/done button.
            entryField.setDoneListener(evt->{

                System.out.println("Received message.");

                if (te != null) {
                    Map extra = new HashMap();
                    extra.put(SEND_ACTION, entryField.getText());
                    System.out.println("Firing event");
                    te.fireEvent(entity, this, extra);

                }
            });
        }
        
        Component sendActionCmp = null;
        if (te != null) {
            sendActionCmp = te.createView(getEntity());
        }
        
        
        
        messagesProp = et != null ? et.findProperty(ChatRoom.messages) : null;
        if (messagesProp == null) {
            if (this.node instanceof ListNode) {
                listNode = (ListNode)node;
            }
            if (entity instanceof EntityList) {
                EntityList entityList = (EntityList)entity;
                Property commentTextProp = entityList.getRowType().findProperty(Comment.text);
                if (commentTextProp != null) {
                    messagesEntity = entityList;
                }
            }
        } else {
            Object messages = entity.get(messagesProp);
            if (messages instanceof EntityList) {
                EntityList entityList = (EntityList)messages;
                Property commentTextProp = entityList.getRowType().findProperty(Comment.text);
                if (commentTextProp != null) {
                    messagesEntity = entityList;
                }
            }
        }
        if (messagesEntity == null) {
            throw new IllegalArgumentException("ChatRoomView requires the entity to either be a list of chat messages, or an entity containing a list of chat messages.");
        }
        participantsProp = et != null ? et.findProperty(ChatRoom.participants) : null;

        if (listNode  == null) {
            listNode = (ListNode)node.findAttribute(ListNode.class);
            if (listNode == null) {
                listNode = new ListNode();
                node.setAttributes(listNode);
            }
        }
        listNode.setAttributes(UI.cellRenderer(new ChatBubbleView.ChatBubbleListCellRenderer()));
        
        EntityEditor messagesList = new EntityEditor(messagesEntity, listNode);
        messagesList.setSafeArea(true);
        messagesList.setUIID("ChatRoomViewMessagesList");
        wrapper.add(CENTER, messagesList);
        
        
        
        Actions chatEntryActions = node.getActions(TEXT_ACTIONS);
        if (!chatEntryActions.isEmpty()) {
            Container cnt = new Container(new GridLayout(chatEntryActions.size()));
            for (ActionNode action : chatEntryActions) {
                action = (ActionNode)action.createProxy(node);
                action.setAttributes(UI.actionStyle(ActionStyle.IconOnly));
                
                cnt.add(action.createView(entity));
            }
            Container south = BorderLayout.centerEastWest(BoxLayout.encloseYCenter(entryFieldBinding), sendActionCmp, cnt);
            south.setUIID("ChatRoomViewSouth");
            south.setSafeArea(true);
            wrapper.add(SOUTH, south);
            
        } else {
            
            
            
            Container south = BorderLayout.centerEastWest(BoxLayout.encloseYCenter(entryFieldBinding), sendActionCmp, null);
            south.setSafeArea(true);
            wrapper.add(SOUTH, south);
        }
        
        add(CENTER, wrapper);
        
        if (form != null) {
            form.setFormBottomPaddingEditingMode(true);
            if (participantsProp != null && participantsProp.getContentType().isEntityList() && !getEntity().isEmpty(participantsProp)) {
                EntityList participantsList = (EntityList)entity.get(participantsProp);
                ViewNode participantsListNode = new ViewNode();
                
                participantsListNode.setParent(node);
                float sizeMM = 5;
                if (CN.isDesktop()) {
                    sizeMM *= 2;
                }
                ProfileAvatarsTitleComponent titleComponent = new ProfileAvatarsTitleComponent(participantsList, participantsListNode, sizeMM);
                
                Toolbar toolbar = form.getToolbar();
                if (toolbar != null) {
                    toolbar.setTitleComponent(titleComponent);
                } else {
                    Label oldTitleComponent = form.getTitleComponent();
                    Container titleArea = form.getTitleComponent().getParent();
                    titleArea.replace(oldTitleComponent, titleComponent, null);
                }
                //form.getTitleComponent().getParent().replace(form.getTitleComponent(), titleComponent, null);
            }
        }
        
       
        
    }
    
    

    @Override
    public void bind() {
        super.bind();
        
    }
    
    

    @Override
    public void update() {
        
    }

    @Override
    public void commit() {
        
    }

    @Override
    public Node getViewNode() {
        return node;
    }
    
    /**
     * A factory that can be added to a view node to "turn" it into a ChatRoomView.
     */
    public static class ChatRoomViewFactory implements EntityViewFactory {

        @Override
        public EntityView createView(Entity entity, ViewNode node) {
            return new ChatRoomView(entity, node, null);
        }
        
    }
    
    
    /**
     * A reference view model for the ChatRoomView.  You do NOT need to use this class
     * for your view model as long as your own view model includes at least properties
     * with the {@link #messagesTag} and {@link #inputBufferTag}.
     */
    public static class ViewModel extends Entity {
        
        /**
         * InputBuffer property, which is bound to the text field for entering
         * messages.  
         * @see #inputBufferTag
         */
        public static StringProperty inputBuffer;
        
        /**
         * Messages property, which contains a list of chat messages to display
         * in the chat room.  See {@link ChatBubbleView.ViewModel} for a reference
         * implementation of an entity that can be used as the model for a chat message,
         * and description of which tags a custom entity must implement to be used
         * as a chat message view model.
         * 
         * @see #messagesTag
         * @see ChatBubbleView.ViewModel
         */
        public static ListProperty messages;
                
        /**
         * Participants property, which contains a list of participants in this chat room.
         * Individual participants can be any entity type that implements the {@link Thing#name}
         * tag.
         */
        public static ListProperty participants;
        
        /**
         * A list of chat messages
         */
        public static class ChatMessages extends EntityList {}
        
        /**
         * A list of participants in a chat room.
         */
        public static class Participants extends EntityList {}
        
        /**
         * Tag used to mark the "input buffer" property on an entity.  This is the one
         * *required* tag for an entity that you want to use as a view model for a
         * ChatRoomView.
         */
        public static final Tag inputBufferTag = ChatRoom.inputBuffer;
        
        /**
         * Tag used to mark the "participants" property of an entity.  This is an optional
         * tag.  If present, the ChatRoomView will be able to display avatars of the chatroom's
         * participants in the header.
         */
        public static final Tag participantsTag = ChatRoom.participants;
        
        /**
         * Tag used to mark the "messages" property of an entity.  This is a required
         * tag, and the property that is tagged with this should be an entity list
         * containing "chat message" entities.  See {@link ChatBubbleView.ViewModel} for a 
         * reference implementation of a chat message entity, and instructions on using 
         * your own custom entities for chat messages.
         */
        public static final Tag messagesTag = ChatRoom.messages;
        
        private static final EntityType TYPE = new EntityType() {{
            inputBuffer = string(tags(inputBufferTag));
            messages = list(ChatMessages.class, tags(messagesTag));
            participants = list(Participants.class, tags(participantsTag));
        }};
        
        {
            setEntityType(TYPE);
            set(messages, new ChatMessages());
            set(participants, new Participants());
            set(inputBuffer, "");
        }
        
        /**
         * Sets the contents of the input buffer property.  The input buffer property
         * is bound to the text field that allows the user to enter text messages.
         * @param string
         * @return 
         */
        public ViewModel inputBuffer(String string) {
            set(inputBuffer, string);
            return this;
        }
        
        /**
         * Adds chat messages to the chat room.  
         * @param entities "Chat message" entities to add.  See {@link ChatBubbleView.ViewModel} for a 
         * reference implementation of a "chat message" entity, and instructions on using custom entities
         * as "chat messages".
         * @return Self for chaining.
         */
        public ViewModel addMessages(Entity... entities) {
            ChatMessages msgs = (ChatMessages)get(messages);
            for (Entity e : entities) {
                msgs.add(e);
            }
            return this;
        }
        
        /**
         * Removes chat messages from the chat room.
         * @param entities "Chat message" entities to add.  See {@link ChatBubbleView.ViewModel} for a 
         * reference implementation of a "chat message" entity, and instructions on using custom entities
         * as "chat messages".
         * @return 
         */
        public ViewModel removeMessages(Entity... entities) {
            ChatMessages msgs = (ChatMessages)get(messages);
            
            for (Entity e : entities) {
                
                msgs.remove(e);
            }
            return this;
        }
        
        /**
         * Adds participants to the chat room.  Individual participants can be any entity type that implements the {@link Thing#name}
         * tag.
         * @param entities Participants to add.  These can be any entity with a property tagged with {@link Thing#name}.
         * @return 
         */
        public ViewModel addParticipants(Entity... entities) {
            Participants l = (Participants)get(participants);
            
            for (Entity e : entities) {
                
                l.add(e);
            }
            return this;
        }
        
        public ViewModel removeParticipants(Entity... entities) {
            Participants l = (Participants)get(participants);
            
            for (Entity e : entities) {
                
                l.remove(e);
            }
            return this;
        }
        
        /**
         * Gets the input buffer string.
         * @return 
         */
        public String getInputBuffer() {
            return get(inputBuffer);
        }
        
        /**
         * Gets the participants. Individual participants can be any entity type that implements the {@link Thing#name}
         * tag.
         * @return 
         */
        public Participants getParticipants() {
            return (Participants)get(participants);
        }
        
        /**
         * Gets the chat messages in this room.  See {@link ChatBubbleView.ViewModel} for a 
         * reference implementation of a "chat message" entity, and instructions on using custom entities
         * as "chat messages".
         * @return 
         */
        public ChatMessages getMessages() {
            return (ChatMessages)get(messages);
        }
    }
    
}
