import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Alexander Worton on 29/12/2016.
 */
public class ContactManagerImplTestFlush {

    private final ContactManagerImplTestData data;

    {
        data = new ContactManagerImplTestData();
    }

    //input data
    //flush
    //reload
    //check data

    private void flushAndReload(){
        data.getManager().flush();
        data.setManager(new ContactManagerImpl());
    }

    @Test
    public void testRestoreOfContacts(){
        int initialSize = data.getManager().getContacts("").size();
        String name1 = "Name for the test", notes1 = "NOTES";
        String name2 = "Alt name", notes2 = "Some notes";
        int id1 = data.getManager().addNewContact(name1, notes1);
        int id2 = data.getManager().addNewContact(name2, notes2);

        flushAndReload();

        Contact contact1 = (Contact)data.getManager().getContacts(id1).toArray()[0];
        Contact contact2 = (Contact)data.getManager().getContacts(id2).toArray()[0];

        assertNotNull(contact1);
        assertNotNull(contact2);
        assertEquals(id1, contact1.getId());
        assertEquals(id2, contact2.getId());
        assertEquals(name1, contact1.getName());
        assertEquals(name2, contact2.getName());
        assertEquals(notes1, contact1.getNotes());
        assertEquals(notes2, contact2.getNotes());
        assertEquals(initialSize+2, data.getManager().getContacts("").size());
    }

    @Test
    public void testRestoreOfMeetings(){
        Contact selectedContact = (Contact)data.getPopulatedSet().toArray()[2];
        Calendar selectedPastDate = DateFns.getPastDate(12);
        Calendar selectedFutureDate = DateFns.getFutureDate(12);

        int initialPastSize = data.getManager().getPastMeetingListFor(selectedContact).size();
        int initialFutureSize = data.getManager().getFutureMeetingList(selectedContact).size();

        int id1 = data.getManager().addNewPastMeeting(data.getPopulatedSet(), selectedPastDate, "");
        int id2 = data.getManager().addFutureMeeting(data.getPopulatedSet(), selectedFutureDate);

        flushAndReload();

        Contact newSelectedContact = (Contact)data.getManager().getContacts(selectedContact.getId()).toArray()[0];

        Meeting pastMeeting = data.getManager().getPastMeeting(id1);
        Meeting futureMeeting = data.getManager().getFutureMeeting(id2);

        data.getManager().addNewPastMeeting(data.getPopulatedSet(), selectedPastDate, "");
        data.getManager().addFutureMeeting(data.getPopulatedSet(), selectedFutureDate);

        assertNotNull(pastMeeting);
        assertNotNull(futureMeeting);
        assertEquals(id1, pastMeeting.getId());
        assertEquals(id2, futureMeeting.getId());
        assertEquals(data.getPopulatedSet().size(), pastMeeting.getContacts().size());
        assertEquals(data.getPopulatedSet().size(), futureMeeting.getContacts().size());
        assertEquals(selectedPastDate, pastMeeting.getDate());
        assertEquals(selectedFutureDate, futureMeeting.getDate());
        assertEquals(initialPastSize+2, data.getManager().getPastMeetingListFor(newSelectedContact).size());
        assertEquals(initialFutureSize+2, data.getManager().getFutureMeetingList(newSelectedContact).size());

    }

    @Test
    public void testRestoreOfMeetingId(){
        Calendar selectedFutureDate = DateFns.getFutureDate(48);
        int id1 = data.getManager().addFutureMeeting(data.getPopulatedSet(), selectedFutureDate);
        flushAndReload();
        int id2 = data.getManager().addFutureMeeting(data.getPopulatedSet(), selectedFutureDate);

        assertTrue(id2 > id1);
    }

    @Test
    public void testRestoreOfContactId(){
        String name1 = "Contact ID 1", notes1 = "notes1";
        String name2 = "Contact ID 2", notes2 = "notes2";
        int id1 = data.getManager().addNewContact(name1, notes1);
        flushAndReload();
        int id2 = data.getManager().addNewContact(name2, notes2);

        assertTrue(id2 > id1);
    }

    @Test
    public void testNoWriteExceptionRaised(){
        File file = new File("contacts.txt");
        if(!file.exists()){
            try{
                file.createNewFile();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }

        file.setWritable(false);
        file.setReadable(false);

        flushAndReload();


    }


}
