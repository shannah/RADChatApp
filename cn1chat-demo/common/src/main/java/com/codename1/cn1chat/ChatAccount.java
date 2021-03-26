
package com.codename1.cn1chat;
import com.codename1.rad.models.Entity;
import com.codename1.rad.models.EntityType;
import static com.codename1.rad.models.EntityType.tags;
import com.codename1.rad.models.StringProperty;
import com.codename1.rad.schemas.Person;
import com.codename1.rad.schemas.Thing;

/**
 * View model for an account profile.
 * @author shannah
 */
public class ChatAccount extends Entity {
    
    // The name property
    public static StringProperty name, thumbnailUrl, phone;
    
    private static final EntityType TYPE = new EntityType() {{
        name = string(tags(Thing.name));
        thumbnailUrl = string(tags(Thing.thumbnailUrl));
        phone = string(tags(Person.telephone));
    }};
    {
        setEntityType(TYPE);
    }
    
    public ChatAccount(String nm, String thumb, String phoneNum) {
        set(name, nm);
        set(thumbnailUrl, thumb);
        set(phone, phoneNum);
    }
    
}
