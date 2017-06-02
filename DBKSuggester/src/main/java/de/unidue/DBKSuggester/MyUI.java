package de.unidue.DBKSuggester;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBoxGroup;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.VerticalLayout;

/**
 * This UI is the application entry point. A UI may either represent a browser window 
 * (or tab) or some part of a html page where a Vaadin application is embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is intended to be 
 * overridden to add component to the user interface and initialize non-component functionality.
 */
@Theme("mytheme")
public class MyUI extends UI {

	Megadoc megadoc = new Megadoc();
	
    final VerticalLayout layout = new VerticalLayout();
    
    DocUploader receiver = new DocUploader();
    private File tempFile;
    
    Label pageTitle = new Label("<h1>Category Suggestion Tool</h1>", ContentMode.HTML);
    
    Label studyContent = new Label();
    
    VerticalLayout categoriesContainer = new VerticalLayout();
    
    Label explainSuggestions = new Label("<h3>Suggested Categories</h3>"
    		+ " - Check the ones you want to keep<br>"
    		+ " - Hover to see example documents from each suggested category<br><br>", ContentMode.HTML);
    
    HorizontalLayout suggestionsContainer = new HorizontalLayout();
    
    CheckBoxGroup<String> suggestionBoxCESSDA = new CheckBoxGroup<>("CESSDA");
    CheckBoxGroup<String> suggestionBoxZA = new CheckBoxGroup<>("ZA");
    
    Label explainAddCategories = new Label("<h3>Add more Categories</h3>", ContentMode.HTML);
    
    HorizontalLayout comboBoxContainer = new HorizontalLayout();
    
    VerticalLayout comboBoxContainerCESSDA = new VerticalLayout();
    VerticalLayout comboBoxContainerZA = new VerticalLayout();
    
    List<String> selectedCategoriesCESSDA = new ArrayList<String>();
    List<String> selectedCategoriesZA = new ArrayList<String>();
    
    Label gap = new Label("&nbsp;", ContentMode.HTML);
    
    Button sendButton = new Button("Confirm");
    
    Label confirm = new Label("The Document has been added to the Index! You can upload another one.");
    
    HorizontalLayout indexAndEvalButtons = new HorizontalLayout();
    
    Button indexButton = new Button("Reset Index");
    Button indexNonMegaButton = new Button("Reset Index NonMegadoc");
    Button indexNoFieldsButton = new Button("Reset Index NoFields");
    Button evalButton = new Button("Evaluate");
    Button evalNonMegaButton = new Button("Evaluate NonMegadoc");
    Button evalNoFieldsButton = new Button("Evaluate NoFields");
    
	
    
    @Override
    protected void init(VaadinRequest vaadinRequest) {
        layout.setWidth("100%");
        
        Upload upload = new Upload("", receiver);
        upload.setButtonCaption("Click here to Upload/Change Study");
        upload.setImmediateMode(true);
        upload.addSucceededListener(receiver);
        
        studyContent.setWidth("100%");
        studyContent.setVisible(false);
        
        addComboBoxCESSDA();
        comboBoxContainerCESSDA.setMargin(false);
        
        addComboBoxZA();
        comboBoxContainerZA.setMargin(false);
        
        
        categoriesContainer.setVisible(false);
        categoriesContainer.setSizeFull();
        categoriesContainer.setSpacing(true);
        categoriesContainer.setMargin(false);
        categoriesContainer.addComponent(explainSuggestions);
        suggestionsContainer.addComponent(suggestionBoxCESSDA);
        suggestionsContainer.addComponent(suggestionBoxZA);
        suggestionsContainer.setWidth("1000px");
        categoriesContainer.addComponent(suggestionsContainer);
        categoriesContainer.addComponent(explainAddCategories);
        comboBoxContainer.addComponent(comboBoxContainerCESSDA);
        comboBoxContainer.addComponent(comboBoxContainerZA);
        comboBoxContainer.setWidth("1000px");
        categoriesContainer.addComponent(comboBoxContainer);
        
        sendButton.setVisible(false);
        sendButton.addClickListener(clickEvent -> send());
        
        confirm.setWidth("100%");
        confirm.setVisible(false);
        
        indexButton.addClickListener(clickEvent -> megadoc.resetIndex());
        evalButton.addClickListener(clickEvent -> megadoc.evaluate());
        indexNonMegaButton.addClickListener(clickEvent -> megadoc.nonMegaResetIndex());
        evalNonMegaButton.addClickListener(clickEvent -> megadoc.nonMegaEvaluate());
        indexNoFieldsButton.addClickListener(clickEvent -> megadoc.noFieldsResetIndex());
        evalNoFieldsButton.addClickListener(clickEvent -> megadoc.noFieldsEvaluate());
        
        indexAndEvalButtons.addComponents(indexButton, evalButton, indexNonMegaButton, evalNonMegaButton, indexNoFieldsButton, evalNoFieldsButton);
        
        layout.addComponents(pageTitle, upload, studyContent, categoriesContainer, gap, sendButton, confirm, indexAndEvalButtons);
        
        setContent(layout);
    }
    
    
    class DocUploader implements Receiver, SucceededListener {
        public File file;

        public OutputStream receiveUpload(String filename,
                                          String mimeType) {
        	try {
                tempFile = File.createTempFile(filename, "xml");
                tempFile.deleteOnExit();
                return new FileOutputStream(tempFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
            
        }

        public void uploadSucceeded(SucceededEvent event) {
            // Show the uploaded file in the image viewer
        	Suggestion suggestions = megadoc.makeSuggestion(tempFile);
        	Study study = megadoc.getStudy(tempFile);
        	fillStudyContent(study);
        	
        	suggestionBoxCESSDA.setItems(suggestions.getCessdaCats());
        	suggestionBoxZA.setItems(suggestions.getZaCats());
        	
  
        	suggestionBoxCESSDA.setDescription(suggestions.getTooltipC(), ContentMode.HTML);
        	suggestionBoxZA.setDescription(suggestions.getTooltipZ(), ContentMode.HTML);
        	
        	categoriesContainer.setVisible(true);
        	sendButton.setVisible(true);
        	confirm.setVisible(false);
        }
    };
    
    
    private void fillStudyContent(Study study) {
    	studyContent.setValue( 
    			"<h2>Study:</h2>" +
    			"<b>ID: </b>" + study.id() + "<br>" +
    			"<b>Titel Deutsch: </b>" + study.titleDE() + "<br>" +
    			"<b>Title English: </b>" + study.titleEN() + "<br>" +
    			"<b>Creators: </b>" + study.creators() + "<br>" +
    			"<b>Inhalt Deutsch: </b>" + study.contentDE() + "<br>" +
    			"<b>Content English: </b>" + study.contentEN() + "<br>"
    			);
    	studyContent.setContentMode(ContentMode.HTML);

    	studyContent.setVisible(true);
    }
    
    private void addComboBoxCESSDA(){
    	comboBoxContainerCESSDA.addComponent(new ComboBox<String>() {{ 
    		setItems(megadoc.CESSDATOPICS); 
    		addSelectionListener(event ->addComboBoxCESSDA());
			}});
    }
    
    private void addComboBoxZA(){
    	comboBoxContainerZA.addComponent(new ComboBox<String>() {{ 
    		setItems(megadoc.ZACATEGORIES); 
    		addSelectionListener(event ->addComboBoxZA());
			}});
    }
    
    private void processComboBoxes(ComponentContainer layout, List<String> selcat) {
        Iterator<Component> componentIterator = layout.getComponentIterator();
        while (componentIterator.hasNext()) {
            Component component = componentIterator.next();

            if (component instanceof ComboBox) {
            	
            	String value = (String) ((ComboBox) component).getValue();
            	
            	if (value != null) {
            		if (!selcat.contains(value)) {
            			selcat.add(value);
            		}
            	}
            }

        }
    }
    
    private void send() {
    	
    	for (String s : suggestionBoxCESSDA.getSelectedItems()) {
    		selectedCategoriesCESSDA.add(s);
    	};
    	for (String s : suggestionBoxZA.getSelectedItems()) {
    		selectedCategoriesZA.add(s);
    	};
    	
    	processComboBoxes(comboBoxContainerCESSDA, selectedCategoriesCESSDA);
    	processComboBoxes(comboBoxContainerZA, selectedCategoriesZA);
    	
    	megadoc.indexStudy(selectedCategoriesCESSDA, selectedCategoriesZA);
    	
    	studyContent.setVisible(false);
    	categoriesContainer.setVisible(false);
    	sendButton.setVisible(false);
    	comboBoxContainerCESSDA.removeAllComponents();
    	comboBoxContainerZA.removeAllComponents();
    	addComboBoxCESSDA();
    	addComboBoxZA();
    	
    	confirm.setValue("The Document has been added to the Index under the following categories:<br>CESSDA: " 
    			+ selectedCategoriesCESSDA + "<br>ZA: " + selectedCategoriesZA + "<br>" 
    			+ "You can upload another one.");
    	confirm.setContentMode(ContentMode.HTML);
    	confirm.setVisible(true);
    	
    	selectedCategoriesCESSDA.clear();
    	selectedCategoriesZA.clear();
    }
    
    
    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
    
}
