
package burp;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kumar
 */
public class BurpExtender implements IBurpExtender, IMessageEditorTabFactory {
    
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    
    
    
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        
        callbacks.setExtensionName("GraphQL Beautifier");
        
        callbacks.registerMessageEditorTabFactory(this);
        
    }
    
    @Override
    public IMessageEditorTab createNewInstance(IMessageEditorController controller, boolean editable) {
        return new GraphQLBeauty(controller, editable);
    }
    
    
    class GraphQLBeauty implements IMessageEditorTab {
        private boolean editable;
        private ITextEditor txtInput;
        private byte[] currentMessage;
        
        public GraphQLBeauty(IMessageEditorController controller, boolean editable) {
            this.editable = this.editable;
            
            txtInput = callbacks.createTextEditor();
            txtInput.setEditable(editable);
        }
        
        //
        // implement IMessageEditorTab
        //
        
        @Override
        public String getTabCaption() {
            return "GraphQL Beautifier";
        }
        
        @Override
        public Component getUiComponent() {
            return txtInput.getComponent();
        }
        
        @Override
        public boolean isEnabled(byte[] content, boolean isRequest) {
            if(isRequest) {
                IRequestInfo requestInfo;
                requestInfo = helpers.analyzeRequest(content);
                return requestInfo.getContentType() == IRequestInfo.CONTENT_TYPE_JSON;
            }
            return false;
        }
        
        @Override
        public void setMessage(byte[] content, boolean isRequest) {
            if(content == null) {
                // clear our display
                txtInput.setText(null);
                txtInput.setEditable(false);
            }
            else {
                int bodyOffset = 0;
                IRequestInfo requestInfo = helpers.analyzeRequest(content);
                bodyOffset = requestInfo.getBodyOffset();
                
                // Copy the raw request data
                byte[] requestBody = Arrays.copyOfRange(content, bodyOffset, content.length);
                String str = new String(requestBody);
                char[] chr = str.toCharArray();
                int size = chr.length;
                char[] temp = new char[size];
                int i;
                for(i=0; i<size-1; i++) {
                    if(chr[i] == '\\' && chr[i+1] == 'n') {
                        temp[i] += '\n';
                        i++;
                        continue;
                    }
                    temp[i] = chr[i];
                }
                temp[i] = chr[i];
                str = new String(temp);
                requestBody = helpers.stringToBytes(str);
                txtInput.setText(requestBody);
                txtInput.setEditable(editable);
            }
            
            currentMessage = content;
            
        }
        
        @Override
        public byte[] getMessage() {
            try {
                if(txtInput.isTextModified()) {
                String str = new String(txtInput.getText());
                char[] chr = str.toCharArray();
                int size = chr.length;
                char[] temp = new char[size];
                int i;
                for(i=0; i<size-1; i++) {
                    if(chr[i] == '\\' && chr[i+1] == 'n') {
                        temp[i] += '\n';
                        i++;
                        continue;
                    }
                    temp[i] = chr[i];
                }
                temp[i] = chr[i];
                str = new String(temp);
                byte[] requestBody = helpers.stringToBytes(str);
                IRequestInfo requestInfo = helpers.analyzeRequest(currentMessage);
                return helpers.buildHttpMessage(requestInfo.getHeaders(), requestBody);
                }
            } catch(Exception e) {
                return currentMessage;
            }
            return null;
        }
        
        @Override
        public boolean isModified() {
            return txtInput.isTextModified();
        }
        
        @Override
        public byte[] getSelectedData() {
            return txtInput.getSelectedText();
        }
    }
}
