package com.rfmemail.controller;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.RedirectView;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Thread;
import com.google.api.services.gmail.model.Message;
import com.rfmemail.Info;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

@Controller
public class GoogleMailController implements WebMvcConfigurer {
	private static final String APPLICATION_NAME = "RFM Email Retrival System";
	private static HttpTransport httpTransport;
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static com.google.api.services.gmail.Gmail client;
	private Info myInfo = new Info();
	private String email;
	private String to;
	private String from;
	

	
	GoogleClientSecrets clientSecrets;
	GoogleAuthorizationCodeFlow flow;
	Credential credential;
	
	@Value("${gmail.client.clientId}")
	private String clientId;
	
	@Value("${gmail.client.clientSecret}")
	private String clientSecret;

	@Value("${gmail.client.redirectUri}")
	private String redirectUri;
	
	
	@GetMapping("/index")
	public String index(Model model) {
		model.addAttribute("info", new Info());
		return "index";
	}
	
	@PostMapping("/index")
	public String result(@Valid @ModelAttribute Info info,BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			return "error";
		}
		else {
			email = info.getEmail();
			to = info.getTo();
			from = info.getFrom();
		}
	
		return "results";
		
	}
	
	@RequestMapping(value = "/login/gmail",method=RequestMethod.GET)
	public RedirectView googleConnectionStatus(HttpServletRequest request) throws Exception {
		return new RedirectView(authorize());
	}

	@RequestMapping(value = "/login/gmailCallback", method = RequestMethod.GET, params = "code")
	public ResponseEntity<String> oauth2Callback(@RequestParam(value = "code") String code) {
		JSONObject json = new JSONObject();
		JSONArray arr = new JSONArray();
		
		final org.thymeleaf.context.Context ctx = new org.thymeleaf.context.Context();
		
		

		
		

		// String message;
		try {
			TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
			credential = flow.createAndStoreCredential(response, "userID");

			client = new com.google.api.services.gmail.Gmail.Builder(httpTransport, JSON_FACTORY, credential)
					.setApplicationName(APPLICATION_NAME).build();

			String userId = "me";
			String query = "from: "+ email +" AND "+ "after:" + from + " before:" + to;
			
			//******************LIST OF MESSAGES*****************\\
			//if(email.contains("hackster.io")){}
			ListMessagesResponse MsgResponse = client.users().messages().list(userId).setQ(query).execute();
			List<Message> messages = new ArrayList<>();
			System.out.println("message length:" + MsgResponse.getMessages().size());
			for (Message msg : MsgResponse.getMessages()) {

				messages.add(msg);

				Message message = client.users().messages().get(userId, msg.getId()).execute();
				System.out.println("snippet :" + message.getSnippet());

				arr.put(message.getSnippet());
			}
			//Json array that displays to the screen
			json.put("response", arr);
			for (Message msg : messages) {
				System.out.println("msg: " + msg.toPrettyString());
			}
			
			//***********************LIst Of threads***************\\
//			ListThreadsResponse Threadresponse = client.users().threads().list(userId).setQ(query).execute();
//			List<Thread> threads = new ArrayList<Thread>();
//		    while(Threadresponse.getThreads() != null) {
//		      threads.addAll(Threadresponse.getThreads());
//		      if(Threadresponse.getNextPageToken() != null) {
//		        String pageToken = Threadresponse.getNextPageToken();
//		        Threadresponse = client.users().threads().list(userId).setQ(query).setPageToken(pageToken).execute();
//		      } else {
//		        break;
//		      }
//		      
//		    }
//		    System.out.println("Thread Length: "+Threadresponse.getThreads().size());
//
//		    for(Thread thread : threads) {
//		      //System.out.println(thread.toPrettyString());
//		      System.out.println(thread.getSnippet());
//		    }

		} catch (Exception e) {

			System.out.println("exception cached ");
			e.printStackTrace();
			
		}
		ctx.setVariable("myJson", json.toString());
		final String htmlContent = htmlTemplateEngine().process("index2", ctx);
		
		return ResponseEntity.ok().body(htmlContent);
		//return new ResponseEntity<>(json.toString(), HttpStatus.OK);
	}
	public static String[] toStringArray(JSONArray array) {
	    if(array==null)
	        return null;

	    String[] arr=new String[array.length()];
	    for(int i=0; i<arr.length; i++) {
	        arr[i]=array.optString(i);
	    }
	    return arr;
	}
	public static void saveMessage(String[] arr) throws IOException {
		FileWriter wrt = new FileWriter("result.txt");
		for(String str:arr) {
			wrt.write(str);
		}
		wrt.close();
	}
	
	private String authorize() throws Exception {
		AuthorizationCodeRequestUrl authorizationUrl;
		if (flow == null) {
			Details web = new Details();
			web.setClientId(clientId);
			web.setClientSecret(clientSecret);
			clientSecrets = new GoogleClientSecrets().setWeb(web);
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
					Collections.singleton(GmailScopes.GMAIL_READONLY)).build();
		}
		authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri);

		System.out.println("gmail authorizationUrl ->" + authorizationUrl);
		return authorizationUrl.build();
	}
	@Bean
	public TemplateEngine htmlTemplateEngine() {
	final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
	templateEngine.addTemplateResolver(htmlTemplateResolver());

	return templateEngine;
	}

	private ITemplateResolver htmlTemplateResolver() {
	final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
	templateResolver.setResolvablePatterns(Collections.singleton("html/*"));
	templateResolver.setPrefix("templates/");
	templateResolver.setSuffix(".html");
	templateResolver.setTemplateMode(TemplateMode.HTML);
	templateResolver.setCharacterEncoding("utf-8");
	templateResolver.setCacheable(false);

	return templateResolver;
	}
	
}
