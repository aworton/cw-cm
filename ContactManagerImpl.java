import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Alexander Worton on 27/12/2016.
 */
public class ContactManagerImpl implements ContactManager{

    private int lastContactId;
    private Map<Integer, Contact> contacts;

    private int lastMeetingId;
    private Map<Integer, Meeting> meetings;

    private String fileName;
    private File file;

    {
        lastContactId = 0;
        lastMeetingId = 0;
        contacts = new HashMap<>();
        meetings = new HashMap<>();
        fileName = "contacts.txt";
        file = new File(fileName);
    }

    public ContactManagerImpl(){
        //load data from file if exists
        readDumpFromFile();
    }

    @Override
    public int addFutureMeeting(Set<Contact> contacts, Calendar date) {
        validateAddNewFutureMeeting(contacts, date);
        return createNewFutureMeeting(contacts, date);
    }

    private void validateAddNewFutureMeeting(Set<Contact> contacts, Calendar date) {
        Validation.validateObjectNotNull(contacts, "Contacts");
        Validation.validateSetPopulated(contacts, "Contacts");
        Validation.validateObjectNotNull(date, "Date");
        Validation.validateDateInFuture(date);
        Validation.validateAllContactsKnown(contacts, this.contacts); //last as computationally intensive O(n)
    }

    private int createNewFutureMeeting(Set<Contact> contacts, Calendar date){
        int id = getNewMeetingId();
        Meeting meeting = new FutureMeetingImpl(id, date, contacts);
        this.meetings.put(id, meeting);
        return id;
    }

    @Override
    public PastMeeting getPastMeeting(int id) {
        Meeting meeting = this.meetings.get(id);
        if(meeting == null) return null;
        Validation.validateStateInPast(meeting.getDate());

        if(!meeting.getClass().equals(PastMeetingImpl.class)) //enforces the event must have occurred and had notes added
            return null;

        return (PastMeeting)meeting;
    }

    @Override
    public FutureMeeting getFutureMeeting(int id) {
        Meeting meeting = this.meetings.get(id);
        if(meeting != null)
            Validation.validateStateInFuture(meeting.getDate());

        return (FutureMeeting)meeting;
    }

    @Override
    public Meeting getMeeting(int id) {
        return meetings.get(id);
    }

    @Override
    public List<Meeting> getFutureMeetingList(Contact contact) {
        Validation.validateObjectNotNull(contact, "Contact");
        Validation.validateContactKnown(contact, this.contacts); //last as computationally intensive O(n)
        return getSortedElementsFromMapAsList(this.meetings,
                (k,v) ->
                        v.getContacts().contains(contact)
                        && v.getDate().after(Calendar.getInstance()),
                Comparator.comparing(Meeting::getDate)
        );
    }

    @Override
    public List<Meeting> getMeetingListOn(Calendar date) {
        Validation.validateObjectNotNull(date);
        LocalDate dateOnly = getDateOnly(date);
        List<Meeting> meetingList = getSortedElementsFromMapAsList(this.meetings,
                (k,v) -> getDateOnly(v.getDate()).equals(dateOnly),
                Comparator.comparing(Meeting::getDate));
        return meetingList;
    }

    private LocalDate getDateOnly(Calendar date) {
        return LocalDate.from(date.getTime()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate());
    }

    @Override
    public List<PastMeeting> getPastMeetingListFor(Contact contact) {
        Validation.validateObjectNotNull(contact);
        Validation.validateContactKnown(contact, this.contacts); //last as computationally intensive O(n)
        List<PastMeeting> meetingList = getPastMeetingsFromMapAsList(this.meetings,
                meeting -> meeting.getContacts().contains(contact)
                            && meeting.getDate().before(Calendar.getInstance()),
                Comparator.comparing(PastMeeting::getDate));
        return meetingList;
    }

    @Override
    public int addNewPastMeeting(Set<Contact> contacts, Calendar date, String text) {
        validateAddNewPastMeeting(contacts, date, text);
        return createNewPastMeeting(contacts, date, text);
    }

    private void validateAddNewPastMeeting(Set<Contact> contacts, Calendar date, String text){
        Validation.validateObjectNotNull(contacts, "Contacts");
        Validation.validateObjectNotNull(date, "Date");
        Validation.validateDateInPast(date);
        Validation.validateObjectNotNull(text, "Text");
        Validation.validateAllContactsKnown(contacts, this.contacts); //last as computationally intensive O(n)
    }

    private int createNewPastMeeting(Set<Contact> contacts, Calendar date, String text) {
        int id = getNewMeetingId();
        Meeting meeting = new PastMeetingImpl(id, date, contacts, text);
        this.meetings.put(id, meeting);
        return id;
    }

    @Override
    public PastMeeting addMeetingNotes(int id, String text) {
        Validation.validateObjectNotNull(text, "Text");
        Meeting meeting = meetings.get(id);
        Validation.validateArgumentNotNull(meeting, "Meeting");
        Validation.validateStateInPast(meeting.getDate());
        return addNotesToPastMeeting(meeting, text);
    }

    private PastMeeting addNotesToPastMeeting(Meeting meeting, String text) {
        PastMeeting meetingWithNotes = new PastMeetingImpl(meeting.getId(), meeting.getDate(), meeting.getContacts(), text);
        this.meetings.put(meetingWithNotes.getId(), meetingWithNotes); //overwrite previous meeting without notes
        return meetingWithNotes;
    }

    @Override
    public int addNewContact(String name, String notes) {
        Validation.validateStringNotNullOrEmpty(name, "name");
        Validation.validateStringNotNullOrEmpty(notes, "notes");

        int id = getNewContactId();
        this.contacts.put(id, new ContactImpl(id, name, notes));
        return id;
    }

    private int getNewContactId() {
        return ++this.lastContactId;
    }

    private int getNewMeetingId() {
        return ++this.lastMeetingId;
    }

    @Override
    public Set<Contact> getContacts(String name) {
        Validation.validateObjectNotNull(name, "Name");
        if(name.equals(""))
            return getContactsAsSet();

        return getElementsFromMapAsSet(this.contacts, (k, v) -> v.getName().equals(name));
    }

    private Set<Contact> getContactsAsSet() {
        return contacts.values().stream()
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Contact> getContacts(int... ids) {
        Validation.validateSetPopulated(ids, "Contact Ids array");
        Set<Contact> result = getElementsFromMapAsSet(this.contacts, (k, v) -> IntStream.of(ids).anyMatch(i -> i == k));
        Validation.validateArgumentSizeMatch(ids.length, result.size());
        return result;
    }

    private <T> Set<T> getElementsFromMapAsSet(Map<Integer, T> map, BiPredicate<Integer, T> predicate) {
        return map.entrySet().stream()
                .filter(e -> predicate.test(e.getKey(), e.getValue()))
                .map(e -> e.getValue())
                .collect(Collectors.toSet());
    }

    private <T> List<T> getSortedElementsFromMapAsList(Map<Integer, T> map, BiPredicate<Integer, T> predicate, Comparator<T> comparator) {
        return map.entrySet().stream()
                .filter(e -> predicate.test(e.getKey(), e.getValue()))
                .map(e -> e.getValue())
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private List<PastMeeting> getPastMeetingsFromMapAsList(Map<Integer, Meeting> map, Predicate<PastMeeting> predicate, Comparator<PastMeeting> comparator) {
        return map.entrySet().stream()
                .filter(e -> PastMeetingImpl.class.equals(e.getValue().getClass()))
                .map(e -> (PastMeeting)e.getValue())
                .filter(e -> predicate.test(e))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public void flush() {
        ContactManagerDump dump = new ContactManagerDump();
        storeDataInDump(dump);
    }

    private void storeDataInDump(ContactManagerDump dump) {
        dump.setLastContactId(this.lastContactId);
        dump.setLastMeetingId(this.lastMeetingId);
        dump.setContacts(this.contacts);
        dump.setMeetings(this.meetings);
        writeDumpToFile(dump);
    }

    private void writeDumpToFile(ContactManagerDump dump) {
        createFileIfNotExists();
        handleExistingFilePermissions();

        try (FileOutputStream fileStream = new FileOutputStream(this.fileName);
             ObjectOutputStream out = new ObjectOutputStream(fileStream))
        {
            out.writeObject(dump);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private void createFileIfNotExists(){
        if(!file.exists()){
            createFile();
        }
    }

    private void createFile(){
        try{
            file.createNewFile();
            file.setWritable(true);
            file.setReadable(true);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    private void handleExistingFilePermissions(){
        if(!file.canRead())
            file.setReadable(true);

        if(!file.canWrite())
            file.setWritable(true);
    }

    private void readDumpFromFile() {
        if(!file.exists()){
            System.out.println("Failed to read file, does not exist.");
            createFile();
            return;
        }

        handleExistingFilePermissions();

        try(FileInputStream fileStream = new FileInputStream(this.fileName);
            ObjectInputStream in = new ObjectInputStream(fileStream))
        {
            ContactManagerDump restoredData = (ContactManagerDump)in.readObject();
            restoreValuesFromDump(restoredData);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        catch(ClassNotFoundException e){
            e.printStackTrace();
        }
    }

    private void restoreValuesFromDump(ContactManagerDump restored) {
        this.lastContactId = restored.getLastContactId();
        this.lastMeetingId = restored.getLastMeetingId();
        this.contacts = restored.getContacts();
        this.meetings = restored.getMeetings();
    }
}

class ContactManagerDump implements Serializable
{
    private static final long serialVersionUID = 1L;

    private int lastContactId;
    private Map<Integer, Contact> contacts;

    private int lastMeetingId;
    private Map<Integer, Meeting> meetings;

    public int getLastContactId() {
        return lastContactId;
    }

    public void setLastContactId(int lastContactId) {
        this.lastContactId = lastContactId;
    }

    public Map<Integer, Contact> getContacts() {
        return contacts;
    }

    public void setContacts(Map<Integer, Contact> contacts) {
        this.contacts = contacts;
    }

    public int getLastMeetingId() {
        return lastMeetingId;
    }

    public void setLastMeetingId(int lastMeetingId) {
        this.lastMeetingId = lastMeetingId;
    }

    public Map<Integer, Meeting> getMeetings() {
        return meetings;
    }

    public void setMeetings(Map<Integer, Meeting> meetings) {
        this.meetings = meetings;
    }
}
