package dlModel;


import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFaDocumentFormat;
import org.semanticweb.owlapi.formats.RioRDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RioTurtleDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DLsGenerator {
	OWLOntologyManager m = OWLManager.createOWLOntologyManager();
	OWLDataFactory df = OWLManager.getOWLDataFactory();

	public IRI convertStringToIRI(String ns) {
		return IRI.create(ns);
	}

	/**
	 * Simple method to write an OWL structure to <code>System.out</code>. It is basically a wrapper
	 * for the writeOntology method.
	 *
	 * @param ontology the ontology to display
	 */
	public void dumpOWL(OWLOntology ontology) {
		try {
			StringDocumentTarget sdt = new StringDocumentTarget();
			writeOntology(ontology, sdt);
			System.out.println(sdt);
		} catch (Exception e) {
			// this is where Scala would be nice.
			throw new RuntimeException(e);
		}
	}

	public OWLOntology createOntology(String iri) throws OWLOntologyCreationException {
		return createOntology(convertStringToIRI(iri));
	}

	public OWLOntology createOntology(IRI iri) throws OWLOntologyCreationException {
		return m.createOntology(iri);
	}

	public void writeOntology(OWLOntology o, OWLOntologyDocumentTarget documentTarget)
			throws OWLOntologyStorageException {
		m.saveOntology(o, documentTarget);
	}

	public OWLOntology readOntology(OWLOntologyDocumentSource source)
			throws OWLOntologyCreationException {
		return m.loadOntologyFromOntologyDocument(source);
	}

	public OWLClass createClass(String iri) {
		return createClass(convertStringToIRI(iri));
	}

	public OWLClass createClass(IRI iri) {
		return df.getOWLClass(iri);
	}


	public OWLAxiomChange createSubclass(OWLOntology o, OWLClass subclass, OWLClass superclass) {
		return new AddAxiom(o, df.getOWLSubClassOfAxiom(subclass, superclass));
	}

	public void applyChange(OWLAxiomChange... axiom) {
		applyChanges(axiom);
	}

	private void applyChanges(OWLAxiomChange... axioms) {
		m.applyChanges(Arrays.asList(axioms));
	}

	public OWLIndividual createIndividual(String iri) {
		return createIndividual(convertStringToIRI(iri));
	}

	private OWLIndividual createIndividual(IRI iri) {
		return df.getOWLNamedIndividual(iri);
	}

	public OWLAxiomChange associateIndividualWithClass(OWLOntology o,
			OWLClass clazz,
			OWLIndividual individual) {
		return new AddAxiom(o, df.getOWLClassAssertionAxiom(clazz, individual));
	}

	public OWLObjectProperty createObjectProperty(String iri) {
		return createObjectProperty(convertStringToIRI(iri));
	}

	public OWLObjectProperty createObjectProperty(IRI iri) {
		return df.getOWLObjectProperty(iri);
	}

	/**
	 * With ontology o, property in refHolder points to a refTo.
	 *
	 * @param o         The ontology reference
	 * @param property  the data property reference
	 * @param refHolder the container of the property
	 * @param refTo     the class the property points to
	 * @return a patch to the ontology
	 */
	public OWLAxiomChange associateObjectPropertyWithClass(OWLOntology o,
			OWLObjectProperty property,
			OWLClass refHolder,
			OWLClass refTo) {
		OWLClassExpression hasSomeRefTo = df.getOWLObjectSomeValuesFrom(property, refTo);
		OWLSubClassOfAxiom ax = df.getOWLSubClassOfAxiom(refHolder, hasSomeRefTo);
		return new AddAxiom(o, ax);
	}

	/**
	 * With ontology o, an object of class a cannot be simultaneously an object of class b.
	 * This is not implied to be an inverse relationship; saying that a cannot be a b does not
	 * mean that b cannot be an a.
	 *
	 * @param o the ontology reference
	 * @param a the source of the disjunction
	 * @param b the object of the disjunction
	 * @return a patch to the ontology
	 */
	public OWLAxiomChange addDisjointClass(OWLOntology o, OWLClass a, OWLClass b) {
		OWLDisjointClassesAxiom expression = df.getOWLDisjointClassesAxiom(a, b);
		return new AddAxiom(o, expression);
	}

	public OWLAxiomChange addObjectproperty(OWLOntology o, OWLIndividual target, OWLObjectProperty property, OWLIndividual value) {
		OWLObjectPropertyAssertionAxiom prop = df.getOWLObjectPropertyAssertionAxiom(property, target, value);
		return new AddAxiom(o, prop);
	}

	public OWLDataProperty createDataProperty(String iri) {
		return createDataProperty(convertStringToIRI(iri));
	}

	public OWLDataProperty createDataProperty(IRI iri) {
		return df.getOWLDataProperty(iri);
	}

	public OWLAxiomChange addDataToIndividual(OWLOntology o, OWLIndividual individual, OWLDataProperty property, String value) {
		OWLLiteral literal = df.getOWLLiteral(value, OWL2Datatype.XSD_STRING);
		return new AddAxiom(o, df.getOWLDataPropertyAssertionAxiom(property, individual, literal));
	}

	public OWLAxiomChange addDataToIndividual(OWLOntology o, OWLIndividual individual, OWLDataProperty property, boolean value) {
		OWLLiteral literal = df.getOWLLiteral(value);
		return new AddAxiom(o, df.getOWLDataPropertyAssertionAxiom(property, individual, literal));
	}

	public OWLAxiomChange addDataToIndividual(OWLOntology o, OWLIndividual individual, OWLDataProperty property, int value) {
		OWLLiteral literal = df.getOWLLiteral(value);
		return new AddAxiom(o, df.getOWLDataPropertyAssertionAxiom(property, individual, literal));
	}
	public static void shouldCreateOntology() throws Exception {
		// We first need to create an OWLOntologyManager, which will provide a
		// point for creating, loading and saving ontologies. We can create a
		// default ontology manager with the OWLManager class. This provides a
		// common setup of an ontology manager. It registers parsers etc. for
		// loading ontologies in a variety of syntaxes
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		// In OWL 2, an ontology may be named with an IRI (Internationalised
		// Resource Identifier) We can create an instance of the IRI class as
		// follows:
		IRI ontologyIRI = IRI.create("http://www.semanticweb.org/ontologies/dlsontology");
		// Here we have decided to call our ontology
		// "http://www.semanticweb.org/ontologies/myontology" If we publish our
		// ontology then we should make the location coincide with the ontology
		// IRI Now we have an IRI we can create an ontology using the manager
		OWLOntology ontology = manager.createOntology(ontologyIRI);
		System.out.println("Created ontology: " + ontology);
		// In OWL 2 if an ontology has an ontology IRI it may also have a
		// version IRI The OWL API encapsulates ontology IRI and possible
		// version IRI information in an OWLOntologyID Each ontology knows about
		// its ID
		OWLOntologyID ontologyID = ontology.getOntologyID();
		// In this case our ontology has an IRI but does not have a version IRI
		System.out.println("Ontology IRI: " + ontologyID.getOntologyIRI());
		// Our version IRI will be Optional.empty() to indicate that we don't
		// have a version IRI
		// An ontology may not have a version IRI - in this case, we count the
		// ontology as an anonymous ontology. Our ontology does have an IRI so
		// it is not anonymous:
		System.out.println("Anonymous Ontology: " +
				ontologyID.isAnonymous());
		// Once an ontology has been created its ontology ID (Ontology IRI and
		// version IRI can be changed) to do this we must apply a SetOntologyID
		// change through the ontology manager. Lets specify a version IRI for
		// our ontology. In our case we will just "extend" our ontology IRI with
		// some version information. We could of course specify any IRI for our
		// version IRI.
		IRI versionIRI = IRI.create(ontologyIRI + "/version1");
		// Note that we MUST specify an ontology IRI if we want to specify a
		// version IRI
		OWLOntologyID newOntologyID = new OWLOntologyID(Optional.of(ontologyIRI), Optional.of(versionIRI));
		// Create the change that will set our version IRI
		SetOntologyID setOntologyID = new SetOntologyID(ontology, newOntologyID);
		// Apply the change
		manager.applyChange(setOntologyID);
		// We can also just specify the ontology IRI and possibly the version
		// IRI at ontology creation time Set up our ID by specifying an ontology
		// IRI and version IRI
		IRI ontologyIRI2 = IRI.create("http://www.semanticweb.org/ontologies/myontology2");
		IRI versionIRI2 = IRI.create("http://www.semanticweb.org/ontologies/myontology2/newversion");
		OWLOntologyID ontologyID2 = new OWLOntologyID(Optional.of(ontologyIRI2), Optional.of(versionIRI2));
		// Now create the ontology
		OWLOntology ontology2 = manager.createOntology(ontologyID2);
		// Finally, if we don't want to give an ontology an IRI, in OWL 2 we
		// don't have to
		OWLOntology anonOntology = manager.createOntology();
		// This ontology is anonymous
		System.out.println("Anonymous: " + anonOntology.isAnonymous());
	}
	public static void shouldAddClassAssertion() throws Exception {
		// For more information on classes and instances see the OWL 2 Primer
		// http://www.w3.org/TR/2009/REC-owl2-primer-20091027/#Classes_and_Instances
		// In order to say that an individual is an instance of a class (in an
		// ontology), we can add a ClassAssertion to the ontology. For example,
		// suppose we wanted to specify that :Mary is an instance of the class
		// :Person. First we need to obtain the individual :Mary and the class
		// :Person Create an ontology manager to work with
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		// The IRIs used here are taken from the OWL 2 Primer
		String base = "http://example.com/owl/families/";
		PrefixManager pm = new DefaultPrefixManager(null, null, base);
		// Get the reference to the :Person class (the full IRI will be
		// <http://example.com/owl/families/Person>)
		OWLClass person = dataFactory.getOWLClass(":Person", pm);
		// Get the reference to the :Mary class (the full IRI will be
		// <http://example.com/owl/families/Mary>)
		OWLNamedIndividual mary = dataFactory.getOWLNamedIndividual(":Mary", pm);
		// Now create a ClassAssertion to specify that :Mary is an instance of
		// :Person
		OWLClassAssertionAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(person, mary);
		// We need to add the class assertion to the ontology that we want
		// specify that :Mary is a :Person
		OWLOntology ontology = manager.createOntology(IRI.create(base));
		// Add the class assertion
		manager.addAxiom(ontology, classAssertion);
		// Dump the ontology to stdout
		manager.saveOntology(ontology, new StreamDocumentTarget(new ByteArrayOutputStream()));
	}

	public static String[] createDLClasses(){
		String[] DLClasses =new String[84];
		DLClasses[0]="#ALC";
		DLClasses[1]="#ALCH";
		DLClasses[2]="#S";
		DLClasses[3]="#SH";
		DLClasses[4]="#SR";


		//new constructor
		String[] constr=new String[5];
		constr[0]="O";
		constr[1]="I";
		constr[2]="F";
		constr[3]="N";
		constr[4]="Q";

		int alcCounter=5;

		for(int t=0;t<constr.length;t++){
			for(int m=t;m<constr.length;m++){
				if((constr[t]=="F" && constr[m]=="N") || (constr[t]=="F" && constr[m]=="Q") || (constr[t]=="N" && constr[m]=="Q") ||
						(constr[t]=="F" && constr[m]=="F") || (constr[t]=="N" && constr[m]=="N")
						|| (constr[t]=="Q" && constr[m]=="Q")){
					DLClasses[alcCounter]="#ALC"+constr[t]; alcCounter++;	 break;
				} 
				else if(m==t){
					DLClasses[alcCounter]="#ALC"+constr[t];
				}
				else if(m!=t){

					DLClasses[alcCounter]="#ALC"+constr[t]+constr[m];
				}
				alcCounter++;	 
			}
		}

		DLClasses[17]="#ALCIOF";
		DLClasses[18]="#ALCION";
		DLClasses[19]="#ALCIOQ";




		alcCounter=20;


		for(int t=0;t<constr.length;t++){
			for(int m=t;m<constr.length;m++){
				if((constr[t]=="F" && constr[m]=="N") || (constr[t]=="F" && constr[m]=="Q") || (constr[t]=="N" && constr[m]=="Q") ||
						(constr[t]=="F" && constr[m]=="F") || (constr[t]=="N" && constr[m]=="N")
						|| (constr[t]=="Q" && constr[m]=="Q")){
					DLClasses[alcCounter]="#ALCH"+constr[t]; alcCounter++;	 break;
				} 
				else if(m==t){
					DLClasses[alcCounter]="#ALCH"+constr[t];
				}
				else if(m!=t){

					DLClasses[alcCounter]="#ALCH"+constr[t]+constr[m];
				}
				alcCounter++;	 
			}
		}

		DLClasses[32]="#ALCHIOF";
		DLClasses[33]="#ALCHION";
		DLClasses[34]="#ALCHIOQ";

		alcCounter=35;


		for(int t=0;t<constr.length;t++){
			for(int m=t;m<constr.length;m++){
				if((constr[t]=="F" && constr[m]=="N") || (constr[t]=="F" && constr[m]=="Q") || (constr[t]=="N" && constr[m]=="Q") ||
						(constr[t]=="F" && constr[m]=="F") || (constr[t]=="N" && constr[m]=="N")
						|| (constr[t]=="Q" && constr[m]=="Q")){
					DLClasses[alcCounter]="#S"+constr[t]; alcCounter++;	 break;
				} 
				else if(m==t){
					DLClasses[alcCounter]="#S"+constr[t];
				}
				else if(m!=t){

					DLClasses[alcCounter]="#S"+constr[t]+constr[m];
				}
				alcCounter++;	 
			}
		}

		DLClasses[47]="#SIOF";
		DLClasses[48]="#SION";
		DLClasses[49]="#SIOQ";

		alcCounter=50;


		for(int t=0;t<constr.length;t++){
			for(int m=t;m<constr.length;m++){
				if((constr[t]=="F" && constr[m]=="N") || (constr[t]=="F" && constr[m]=="Q") || (constr[t]=="N" && constr[m]=="Q") ||
						(constr[t]=="F" && constr[m]=="F") || (constr[t]=="N" && constr[m]=="N")
						|| (constr[t]=="Q" && constr[m]=="Q")){
					DLClasses[alcCounter]="#SH"+constr[t]; alcCounter++;	 break;
				} 
				else if(m==t){
					DLClasses[alcCounter]="#SH"+constr[t];
				}
				else if(m!=t){

					DLClasses[alcCounter]="#SH"+constr[t]+constr[m];
				}
				alcCounter++;	 
			}
		}

		DLClasses[62]="#SHIOF";
		DLClasses[63]="#SHION";
		DLClasses[64]="#SHIOQ";

		alcCounter=65;


		for(int t=0;t<constr.length;t++){
			for(int m=t;m<constr.length;m++){
				if((constr[t]=="F" && constr[m]=="N") || (constr[t]=="F" && constr[m]=="Q") || (constr[t]=="N" && constr[m]=="Q") ||
						(constr[t]=="F" && constr[m]=="F") || (constr[t]=="N" && constr[m]=="N")
						|| (constr[t]=="Q" && constr[m]=="Q")){
					DLClasses[alcCounter]="#SR"+constr[t]; alcCounter++;	 break;
				} 
				else if(m==t){
					DLClasses[alcCounter]="#SR"+constr[t];
				}
				else if(m!=t){

					DLClasses[alcCounter]="#SR"+constr[t]+constr[m];
				}
				alcCounter++;	 
			}
		}

		DLClasses[77]="#SROF";
		DLClasses[78]="#SRION";
		DLClasses[79]="#SRIOQ";

		//primitive description logics

		DLClasses[80]="#FL0";
		DLClasses[81]="#FL-";
		DLClasses[82]="#EL";
		DLClasses[83]="#EL++";

		return DLClasses;


	}

	public static void main(String[] args){

		//Create the ontology manager
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		//File with an existing ontology -the dl model 
		File file = new File("/home/emira/Desktop/DLModelingFinalVersion.owl");
		//Load the ontology from the file
		OWLOntology ontology = null;
		//try to load the ontology
		try {
			ontology = manager.loadOntologyFromOntologyDocument(file);
		} catch (OWLOntologyCreationException e1) {
			// TODO Auto-generated catch block
			System.out.println("Ontology can not load from the file!");
			e1.printStackTrace();
		}


		//create the data factory from OWLManager
		OWLDataFactory df = OWLManager.getOWLDataFactory();

		//Create an array of Strings to store all the description logics

		String[] DLClasses =new String[84];

		//create the groups of description logics

		DLClasses[0]="#ALC";
		DLClasses[1]="#ALCH";
		DLClasses[2]="#S";
		DLClasses[3]="#SH";
		DLClasses[4]="#SR";


		//Description logics constructor
		String[] constr=new String[5];
		constr[0]="O";
		constr[1]="I";
		constr[2]="F";
		constr[3]="N";
		constr[4]="Q";

		int alcCounter=5;

		for(int t=0;t<constr.length;t++){
			for(int m=t;m<constr.length;m++){
				if((constr[t]=="F" && constr[m]=="N") || (constr[t]=="F" && constr[m]=="Q") || (constr[t]=="N" && constr[m]=="Q") ||
						(constr[t]=="F" && constr[m]=="F") || (constr[t]=="N" && constr[m]=="N")
						|| (constr[t]=="Q" && constr[m]=="Q")){
					DLClasses[alcCounter]="#ALC"+constr[t]; alcCounter++;	 break;
				} 
				else if(m==t){
					DLClasses[alcCounter]="#ALC"+constr[t];
				}
				else if(m!=t){

					DLClasses[alcCounter]="#ALC"+constr[t]+constr[m];
				}
				alcCounter++;	 
			}
		}

		DLClasses[17]="#ALCOIF";
		DLClasses[18]="#ALCOIN";
		DLClasses[19]="#ALCOIQ";




		alcCounter=20;


		for(int t=0;t<constr.length;t++){
			for(int m=t;m<constr.length;m++){
				if((constr[t]=="F" && constr[m]=="N") || (constr[t]=="F" && constr[m]=="Q") || (constr[t]=="N" && constr[m]=="Q") ||
						(constr[t]=="F" && constr[m]=="F") || (constr[t]=="N" && constr[m]=="N")
						|| (constr[t]=="Q" && constr[m]=="Q")){
					DLClasses[alcCounter]="#ALCH"+constr[t]; alcCounter++;	 break;
				} 
				else if(m==t){
					DLClasses[alcCounter]="#ALCH"+constr[t];
				}
				else if(m!=t){

					DLClasses[alcCounter]="#ALCH"+constr[t]+constr[m];
				}
				alcCounter++;	 
			}
		}

		DLClasses[32]="#ALCHOIF";
		DLClasses[33]="#ALCHOIN";
		DLClasses[34]="#ALCHOIQ";

		alcCounter=35;


		for(int t=0;t<constr.length;t++){
			for(int m=t;m<constr.length;m++){
				if((constr[t]=="F" && constr[m]=="N") || (constr[t]=="F" && constr[m]=="Q") || (constr[t]=="N" && constr[m]=="Q") ||
						(constr[t]=="F" && constr[m]=="F") || (constr[t]=="N" && constr[m]=="N")
						|| (constr[t]=="Q" && constr[m]=="Q")){
					DLClasses[alcCounter]="#S"+constr[t]; alcCounter++;	 break;
				} 
				else if(m==t){
					DLClasses[alcCounter]="#S"+constr[t];
				}
				else if(m!=t){

					DLClasses[alcCounter]="#S"+constr[t]+constr[m];
				}
				alcCounter++;	 
			}
		}

		DLClasses[47]="#SOIF";
		DLClasses[48]="#SOIN";
		DLClasses[49]="#SOIQ";

		alcCounter=50;


		for(int t=0;t<constr.length;t++){
			for(int m=t;m<constr.length;m++){
				if((constr[t]=="F" && constr[m]=="N") || (constr[t]=="F" && constr[m]=="Q") || (constr[t]=="N" && constr[m]=="Q") ||
						(constr[t]=="F" && constr[m]=="F") || (constr[t]=="N" && constr[m]=="N")
						|| (constr[t]=="Q" && constr[m]=="Q")){
					DLClasses[alcCounter]="#SH"+constr[t]; alcCounter++;	 break;
				} 
				else if(m==t){
					DLClasses[alcCounter]="#SH"+constr[t];
				}
				else if(m!=t){

					DLClasses[alcCounter]="#SH"+constr[t]+constr[m];
				}
				alcCounter++;	 
			}
		}

		DLClasses[62]="#SHOIF";
		DLClasses[63]="#SHOIN";
		DLClasses[64]="#SHOIQ";

		alcCounter=65;


		for(int t=0;t<constr.length;t++){
			for(int m=t;m<constr.length;m++){
				if((constr[t]=="F" && constr[m]=="N") || (constr[t]=="F" && constr[m]=="Q") || (constr[t]=="N" && constr[m]=="Q") ||
						(constr[t]=="F" && constr[m]=="F") || (constr[t]=="N" && constr[m]=="N")
						|| (constr[t]=="Q" && constr[m]=="Q")){
					DLClasses[alcCounter]="#SR"+constr[t]; alcCounter++;	 break;
				} 
				else if(m==t){
					DLClasses[alcCounter]="#SR"+constr[t];
				}
				else if(m!=t){

					DLClasses[alcCounter]="#SR"+constr[t]+constr[m];
				}
				alcCounter++;	 
			}
		}

		DLClasses[77]="#SROIF";
		DLClasses[78]="#SROIN";
		DLClasses[79]="#SROIQ";

		//primitive description logics

		DLClasses[80]="#FL0";
		DLClasses[81]="#FL-";
		DLClasses[82]="#EL";
		DLClasses[83]="#EL++";

		//Create an array of OWLEntities	    	 
		OWLEntity[] owlEntDL = new OWLEntity[85];
		//Create an array of OWLAxioms
		OWLAxiom[] owlAxiomDL=new OWLAxiom[85];
		String[] dlString=new String[85];
		StringBuilder sb=new StringBuilder();
		//this is for adding the DLs in the axiom
		for(int i=0;i<84;i++){
			owlEntDL[i]=df.getOWLEntity(EntityType.CLASS, IRI.create(DLClasses[i]));
			//for(int j=2;j<owlEntDL[i].toString().length()-1;j++){			
			//sb.append(owlEntDL[i].toString().charAt(j));
			//System.out.println(owlEntDL[i].toString().charAt(j));
			//}
			//dlString[i]=sb.toString();
			//sb.setLength(0);
			//System.out.println(dlString[i]);
			owlAxiomDL[i]=df.getOWLDeclarationAxiom(owlEntDL[i]);
			manager.addAxiom(ontology, owlAxiomDL[i]);
			//if(dlString[i].contains("ALC")){
			//System.out.println("HEllo EMIRA");
			//}
		}




		OWLClassExpression modelingFeature =df.getOWLEntity(EntityType.CLASS, IRI.create("#ModelingFeature"));

		//create a hashet for including all the right handside concepts of 
		//the modelingfeatures
		Set<OWLClassExpression> modelingFeaturesRight=new HashSet<OWLClassExpression>();
		OWLClassExpression atomicConcept =df.getOWLEntity(EntityType.CLASS, IRI.create("#AtomicConcept"));
		OWLClassExpression topConcept =df.getOWLEntity(EntityType.CLASS, IRI.create("#TopConcept"));
		OWLClassExpression disjunction =df.getOWLEntity(EntityType.CLASS, IRI.create("#Disjunction"));
		//OWLClassExpression universalConcept =df.getOWLEntity(EntityType.CLASS, IRI.create("#UniversalConcept"));
		OWLClassExpression bottomConcept =df.getOWLEntity(EntityType.CLASS, IRI.create("#BottomConcept"));
		OWLClassExpression conjunction =df.getOWLEntity(EntityType.CLASS, IRI.create("#Conjunction"));
		OWLClassExpression roleChain =df.getOWLEntity(EntityType.CLASS, IRI.create("#RoleChain"));
		OWLClassExpression roleDisjointness =df.getOWLEntity(EntityType.CLASS, IRI.create("#RoleDisjointness"));
		OWLClassExpression roleHierarchy =df.getOWLEntity(EntityType.CLASS, IRI.create("#RoleHierarchy"));
		OWLClassExpression roleInclusion =df.getOWLEntity(EntityType.CLASS, IRI.create("#RoleInclusion"));
		OWLClassExpression roleInverse =df.getOWLEntity(EntityType.CLASS, IRI.create("#RoleInverse"));
		OWLClassExpression roleTransitivity =df.getOWLEntity(EntityType.CLASS, IRI.create("#RoleTransitivity"));
		OWLClassExpression valueRestriction =df.getOWLEntity(EntityType.CLASS, IRI.create("#ValueRestriction"));
		OWLClassExpression conceptConstructors =df.getOWLEntity(EntityType.CLASS, IRI.create("#ConceptConstructors"));
		OWLClassExpression conceptNegation =df.getOWLEntity(EntityType.CLASS, IRI.create("#ConceptNegation"));
		OWLClassExpression existentialRestriction =df.getOWLEntity(EntityType.CLASS, IRI.create("#ExistentialRestriction"));


		modelingFeaturesRight.add(atomicConcept);
		modelingFeaturesRight.add(topConcept);
		modelingFeaturesRight.add(disjunction);
		//modelingFeaturesRight.add(universalConcept);
		modelingFeaturesRight.add(bottomConcept);
		modelingFeaturesRight.add(conjunction);
		modelingFeaturesRight.add(roleChain);
		modelingFeaturesRight.add(roleDisjointness);
		modelingFeaturesRight.add(roleHierarchy);
		modelingFeaturesRight.add(roleInclusion);
		modelingFeaturesRight.add(roleInverse);
		modelingFeaturesRight.add(roleTransitivity);
		modelingFeaturesRight.add(valueRestriction);
		modelingFeaturesRight.add(conceptConstructors);
		modelingFeaturesRight.add(conceptNegation);
		modelingFeaturesRight.add(existentialRestriction);


		OWLAxiom[] modelingFeaturesAxiom = new OWLAxiom[modelingFeaturesRight.size()];
		//go through all the elements of the set and add them as axioms
		int i=0;
		for (OWLClassExpression oce : modelingFeaturesRight) {
			modelingFeaturesAxiom[i]=df.getOWLSubClassOfAxiom(oce,modelingFeature);
			manager.addAxiom(ontology, modelingFeaturesAxiom[i]);
			i++;
		}


		Set<OWLClassExpression> conceptConstructorsRight=new HashSet<OWLClassExpression>();
		OWLClassExpression functionality =df.getOWLEntity(EntityType.CLASS, IRI.create("#Functionality"));
		OWLClassExpression nominals =df.getOWLEntity(EntityType.CLASS, IRI.create("#Nominals"));
		OWLClassExpression qualifiedNumberRestriction =df.getOWLEntity(EntityType.CLASS, IRI.create("#QualifiedNumberRestriction"));

		conceptConstructorsRight.add(functionality);
		conceptConstructorsRight.add(nominals);
		conceptConstructorsRight.add(qualifiedNumberRestriction);


		OWLAxiom[] conceptConstructorsAxiom = new OWLAxiom[conceptConstructorsRight.size()];
		//go through all the elements of the set and add them as axioms
		int j=0;
		for (OWLClassExpression oce : conceptConstructorsRight) {
			conceptConstructorsAxiom[j]=df.getOWLSubClassOfAxiom(oce,conceptConstructors);
			manager.addAxiom(ontology, conceptConstructorsAxiom[j]);
			j++;
		}


		OWLClassExpression unqualifiedNumberRestriction=df.getOWLEntity(EntityType.CLASS, IRI.create("#UnqualifiedNumberRestriction"));
		OWLAxiom unqualifiedNRAxiom=df.getOWLSubClassOfAxiom(unqualifiedNumberRestriction, qualifiedNumberRestriction);
		manager.addAxiom(ontology, unqualifiedNRAxiom);


		OWLClassExpression atomicNegation=df.getOWLEntity(EntityType.CLASS, IRI.create("#AtomicNegation"));
		OWLAxiom atomicNegationAxiom=df.getOWLSubClassOfAxiom(atomicNegation, conceptNegation);
		manager.addAxiom(ontology, atomicNegationAxiom);

		OWLClassExpression limitedExistentialRestriction=df.getOWLEntity(EntityType.CLASS, IRI.create("#LimitedExistentialRestriction"));
		OWLAxiom limitedExQAxiom=df.getOWLSubClassOfAxiom(limitedExistentialRestriction, existentialRestriction);
		manager.addAxiom(ontology, limitedExQAxiom);

		OWLClassExpression descriptionLogic=df.getOWLEntity(EntityType.CLASS, IRI.create("#DescriptionLogic"));


		OWLObjectProperty hasModelingFeatures= df.getOWLEntity(EntityType.OBJECT_PROPERTY, 
				IRI.create("#hasModelingFeature"));
		OWLAxiom de=df.getOWLDeclarationAxiom(hasModelingFeatures);

		OWLObjectPropertyDomainAxiom domainAxiom = df.getOWLObjectPropertyDomainAxiom(hasModelingFeatures, descriptionLogic);
		OWLObjectPropertyRangeAxiom   rangeAxiom = df.getOWLObjectPropertyRangeAxiom( hasModelingFeatures, modelingFeature);

		manager.addAxiom(ontology,de);
		manager.addAxiom(ontology, domainAxiom);
		manager.addAxiom(ontology,  rangeAxiom);

		OWLClassExpression complexitycl=df.getOWLEntity(EntityType.CLASS, IRI.create("#ComplexityClass"));
		OWLClassExpression exptimecomp=df.getOWLEntity(EntityType.CLASS, IRI.create("#ExpTimeComplete"));
		OWLClassExpression inexptime=df.getOWLEntity(EntityType.CLASS, IRI.create("#InExpTime"));
		OWLClassExpression exptimehard=df.getOWLEntity(EntityType.CLASS, IRI.create("#ExpTimeHard"));

		OWLClassExpression innexptime=df.getOWLEntity(EntityType.CLASS, IRI.create("#InNExpTime"));
		OWLClassExpression nexptimehard=df.getOWLEntity(EntityType.CLASS, IRI.create("#NExpTimeHard"));
		OWLClassExpression nexptimecomp=df.getOWLEntity(EntityType.CLASS, IRI.create("#NExpTimeComplete"));

		
		OWLAxiom axiomexptcom =df.getOWLSubClassOfAxiom(exptimecomp, complexitycl);
		OWLAxiom axiominexptime =df.getOWLSubClassOfAxiom(inexptime, complexitycl);
		OWLAxiom axiomexptimehard =df.getOWLSubClassOfAxiom(exptimehard, complexitycl);
		OWLAxiom axiominnexptime =df.getOWLSubClassOfAxiom(innexptime, complexitycl);
		OWLAxiom axiomnexptimehard =df.getOWLSubClassOfAxiom(nexptimehard, complexitycl);
		OWLAxiom axiomnexptimecomp =df.getOWLSubClassOfAxiom(nexptimecomp, complexitycl);

		manager.addAxiom(ontology, axiomexptcom);
		manager.addAxiom(ontology, axiominexptime);
		manager.addAxiom(ontology, axiomexptimehard);
		manager.addAxiom(ontology, axiominnexptime);
		manager.addAxiom(ontology, axiomnexptimehard);
		manager.addAxiom(ontology, axiomnexptimecomp);

		OWLObjectProperty forConceptSatIsIn= df.getOWLEntity(EntityType.OBJECT_PROPERTY, 
				IRI.create("#forConceptSatIsInCompCl"));
		OWLAxiom dlconc=df.getOWLDeclarationAxiom(forConceptSatIsIn);

		OWLObjectPropertyDomainAxiom domainAxiomdl = df.getOWLObjectPropertyDomainAxiom(forConceptSatIsIn, descriptionLogic);
		OWLObjectPropertyRangeAxiom   rangeAxiomcs = df.getOWLObjectPropertyRangeAxiom( forConceptSatIsIn, complexitycl);

		manager.addAxiom(ontology, dlconc);
		manager.addAxiom(ontology, domainAxiomdl);
		manager.addAxiom(ontology,  rangeAxiomcs);

		//this is for the first general class axiom for ExpTime
		
		OWLClassExpression gcaFirstSome =
				df.getOWLObjectSomeValuesFrom(forConceptSatIsIn,inexptime );
		OWLClassExpression gcaSecondSome =
				df.getOWLObjectSomeValuesFrom(forConceptSatIsIn,exptimehard );
		
		Set<OWLClassExpression> firstConjGCA =new HashSet<OWLClassExpression>();
		firstConjGCA.add(gcaFirstSome);
		firstConjGCA.add(gcaSecondSome);

		OWLClassExpression conGCA=df.getOWLObjectIntersectionOf(firstConjGCA);
		
		OWLClassExpression gcaThirdSome =
				df.getOWLObjectSomeValuesFrom(forConceptSatIsIn,exptimecomp );
		
		
		OWLSubClassOfAxiom axGCA =df.getOWLSubClassOfAxiom(conGCA, gcaThirdSome);
		
		manager.addAxiom(ontology, axGCA);
		
		
		//this is for the second general class axiom for ExpTime
		
		OWLClassExpression gcaFirstSomeTwo =
				df.getOWLObjectSomeValuesFrom(forConceptSatIsIn,innexptime );
		OWLClassExpression gcaSecondSomeTwo =
				df.getOWLObjectSomeValuesFrom(forConceptSatIsIn,nexptimehard );

		Set<OWLClassExpression> firstConjGCATwo =new HashSet<OWLClassExpression>();
		firstConjGCATwo.add(gcaFirstSomeTwo);
		firstConjGCATwo.add(gcaSecondSomeTwo);

		OWLClassExpression conGCATwo=df.getOWLObjectIntersectionOf(firstConjGCATwo);

		OWLClassExpression gcaThirdSomeTwo =
				df.getOWLObjectSomeValuesFrom(forConceptSatIsIn,nexptimecomp );


		OWLSubClassOfAxiom axGCATwo =df.getOWLSubClassOfAxiom(conGCATwo, gcaThirdSomeTwo);

		manager.addAxiom(ontology, axGCATwo);
		
		//this is for defining the description logics
		for(int dlInd=0;dlInd<84;dlInd++){
			for(int jInd=1;jInd<DLClasses[dlInd].length();jInd++){
				sb.append(DLClasses[dlInd].charAt(jInd));
			}
			dlString[dlInd]=sb.toString();
			sb.setLength(0);
			System.out.println(dlString[dlInd]);

			if(dlString[dlInd].contains("ALC")){
				//this is for ALC

				Set<OWLClassExpression> firstDisjALC=new HashSet<OWLClassExpression>();
				firstDisjALC.add(atomicConcept);
				firstDisjALC.add(conceptNegation);
				firstDisjALC.add(topConcept);
				firstDisjALC.add(bottomConcept);
				firstDisjALC.add(disjunction);
				firstDisjALC.add(valueRestriction);
				firstDisjALC.add(conjunction);
				firstDisjALC.add(existentialRestriction);

				if(dlString[dlInd].contains("I")){
					firstDisjALC.add(roleInverse);
				}
				if(dlString[dlInd].contains("O")){
					firstDisjALC.add(nominals);
				}
				if(dlString[dlInd].contains("N")){
					firstDisjALC.add(unqualifiedNumberRestriction);
				}
				if(dlString[dlInd].contains("F")){
					firstDisjALC.add(functionality);
				}
				if(dlString[dlInd].contains("Q")){
					firstDisjALC.add(qualifiedNumberRestriction);
				}
				if(dlString[dlInd].contains("H")){
					firstDisjALC.add(roleInclusion);
				}
				OWLClassExpression disjunctionOfConstructorsALC =df.getOWLObjectUnionOf(firstDisjALC);

				OWLClassExpression onlyConstrALC=df.getOWLObjectAllValuesFrom(hasModelingFeatures, disjunctionOfConstructorsALC);

				Set<OWLClassExpression> firstConjALC =new HashSet<OWLClassExpression>();
				firstConjALC.add(descriptionLogic);
				firstConjALC.add(onlyConstrALC);

				OWLClassExpression conALC=df.getOWLObjectIntersectionOf(firstConjALC);

				Set<OWLClassExpression> finalonALC=new HashSet<OWLClassExpression>();
				finalonALC.add(df.getOWLClass(DLClasses[dlInd]));
				finalonALC.add(conALC);

				OWLAxiom finalALCAxiom = df.getOWLEquivalentClassesAxiom(finalonALC);
				manager.addAxiom(ontology, finalALCAxiom);
				
				
				//this is for complexity
				if((dlString[dlInd].contains("Q") && dlString[dlInd].contains("I") && dlString[dlInd].contains("O")
						) ||(dlString[dlInd].contains("N") && dlString[dlInd].contains("I") && dlString[dlInd].contains("O")
								) || (dlString[dlInd].contains("F") && dlString[dlInd].contains("I") && dlString[dlInd].contains("O")
										)){
					//for inExpTime
				OWLClassExpression hasComplexitySome =
						df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, innexptime);
				
				OWLSubClassOfAxiom ax =df.getOWLSubClassOfAxiom(df.getOWLClass(DLClasses[dlInd]), hasComplexitySome);
				
				manager.addAxiom(ontology, ax);
				
				//for ExpTimeHard
				OWLClassExpression hasComplexitySomeHard =
						df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, nexptimehard);
				
				OWLSubClassOfAxiom axhard =df.getOWLSubClassOfAxiom(df.getOWLClass(DLClasses[dlInd]), hasComplexitySomeHard);
				
				manager.addAxiom(ontology, axhard);
				
				}
				else{
					//for inNExpTime
					OWLClassExpression hasComplexitySome =
							df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, inexptime);
					
					OWLSubClassOfAxiom ax =df.getOWLSubClassOfAxiom(df.getOWLClass(DLClasses[dlInd]), hasComplexitySome);
					
					manager.addAxiom(ontology, ax);
					
					//for NExpTimeHard
					
					OWLClassExpression hasComplexitySomeHard =
							df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, exptimehard);
					
					OWLSubClassOfAxiom axhard =df.getOWLSubClassOfAxiom(df.getOWLClass(DLClasses[dlInd]), hasComplexitySomeHard);
					
					manager.addAxiom(ontology, axhard);
				}
				
			}
			if(dlString[dlInd].contains("S")){
				if(dlString[dlInd].contains("R")){
					//this is for SR

					Set<OWLClassExpression> firstDisjSR=new HashSet<OWLClassExpression>();
					firstDisjSR.add(atomicConcept);
					firstDisjSR.add(conceptNegation);
					firstDisjSR.add(topConcept);
					firstDisjSR.add(bottomConcept);
					firstDisjSR.add(disjunction);
					firstDisjSR.add(valueRestriction);
					firstDisjSR.add(conjunction);
					firstDisjSR.add(existentialRestriction);
					firstDisjSR.add(roleTransitivity);
					firstDisjSR.add(roleChain);
					firstDisjSR.add(roleDisjointness);

					if(dlString[dlInd].contains("I")){
						firstDisjSR.add(roleInverse);
					}
					if(dlString[dlInd].contains("O")){
						firstDisjSR.add(nominals);
					}
					if(dlString[dlInd].contains("N")){
						firstDisjSR.add(unqualifiedNumberRestriction);
					}
					if(dlString[dlInd].contains("F")){
						firstDisjSR.add(functionality);
					}
					if(dlString[dlInd].contains("Q")){
						firstDisjSR.add(qualifiedNumberRestriction);
					}

					OWLClassExpression disjunctionOfConstructorsSR =df.getOWLObjectUnionOf(firstDisjSR);

					OWLClassExpression onlyConstrSR=df.getOWLObjectAllValuesFrom(hasModelingFeatures, disjunctionOfConstructorsSR);

					Set<OWLClassExpression> firstConjSR =new HashSet<OWLClassExpression>();
					firstConjSR.add(descriptionLogic);
					firstConjSR.add(onlyConstrSR);

					OWLClassExpression conSR=df.getOWLObjectIntersectionOf(firstConjSR);

					Set<OWLClassExpression> finalonSR=new HashSet<OWLClassExpression>();
					finalonSR.add(df.getOWLClass(DLClasses[dlInd]));
					finalonSR.add(conSR);

					OWLAxiom finalSRAxiom = df.getOWLEquivalentClassesAxiom(finalonSR);
					manager.addAxiom(ontology, finalSRAxiom);
					
					
					//this is for complexity-everything NExpTimeHard
					//if((dlString[dlInd].contains("Q") && dlString[dlInd].contains("I") && dlString[dlInd].contains("O")
						//	) ||(dlString[dlInd].contains("N") && dlString[dlInd].contains("I") && dlString[dlInd].contains("O")
							//		) || (dlString[dlInd].contains("F") && dlString[dlInd].contains("I") && dlString[dlInd].contains("O")
								//			)){
					OWLClassExpression hasComplexitySome =
							df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, nexptimehard);
					
					OWLSubClassOfAxiom ax =df.getOWLSubClassOfAxiom(df.getOWLClass(DLClasses[dlInd]), hasComplexitySome);
					
					manager.addAxiom(ontology, ax);
					//}
					//else{
						//OWLClassExpression hasComplexitySome =
							//	df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, inexptime);
						
						//OWLSubClassOfAxiom ax =df.getOWLSubClassOfAxiom(df.getOWLClass(DLClasses[dlInd]), hasComplexitySome);
						
						//manager.addAxiom(ontology, ax);
					//}

				}
				else{ //this is for S and SH
					//this is for S

					Set<OWLClassExpression> firstDisjS=new HashSet<OWLClassExpression>();
					firstDisjS.add(atomicConcept);
					firstDisjS.add(conceptNegation);
					firstDisjS.add(topConcept);
					firstDisjS.add(bottomConcept);
					firstDisjS.add(disjunction);
					firstDisjS.add(valueRestriction);
					firstDisjS.add(conjunction);
					firstDisjS.add(existentialRestriction);
					firstDisjS.add(roleTransitivity);

					if(dlString[dlInd].contains("I")){
						firstDisjS.add(roleInverse);
					}
					if(dlString[dlInd].contains("O")){
						firstDisjS.add(nominals);
					}
					if(dlString[dlInd].contains("N")){
						firstDisjS.add(unqualifiedNumberRestriction);
					}
					if(dlString[dlInd].contains("F")){
						firstDisjS.add(functionality);
					}
					if(dlString[dlInd].contains("Q")){
						firstDisjS.add(qualifiedNumberRestriction);
					}
					if(dlString[dlInd].contains("H")){
						firstDisjS.add(roleInclusion);
					}

					OWLClassExpression disjunctionOfConstructorsS =df.getOWLObjectUnionOf(firstDisjS);

					OWLClassExpression onlyConstrS=df.getOWLObjectAllValuesFrom(hasModelingFeatures, disjunctionOfConstructorsS);

					Set<OWLClassExpression> firstConjS =new HashSet<OWLClassExpression>();
					firstConjS.add(descriptionLogic);
					firstConjS.add(onlyConstrS);

					OWLClassExpression conS=df.getOWLObjectIntersectionOf(firstConjS);

					Set<OWLClassExpression> finalonS=new HashSet<OWLClassExpression>();
					finalonS.add(df.getOWLClass(DLClasses[dlInd]));
					finalonS.add(conS);

					OWLAxiom finalSAxiom = df.getOWLEquivalentClassesAxiom(finalonS);
					manager.addAxiom(ontology, finalSAxiom);

					//this is for complexity
					if((dlString[dlInd].contains("Q") && dlString[dlInd].contains("I") && dlString[dlInd].contains("O")
							) ||(dlString[dlInd].contains("N") && dlString[dlInd].contains("I") && dlString[dlInd].contains("O")
									) || (dlString[dlInd].contains("F") && dlString[dlInd].contains("I") && dlString[dlInd].contains("O")
											)){
						//for inExpTime
					OWLClassExpression hasComplexitySome =
							df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, inexptime);
					
					OWLSubClassOfAxiom ax =df.getOWLSubClassOfAxiom(df.getOWLClass(DLClasses[dlInd]), hasComplexitySome);
					
					manager.addAxiom(ontology, ax);
					
					//for ExpTimeHard
					OWLClassExpression hasComplexitySomeHard =
							df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, exptimehard);
					
					OWLSubClassOfAxiom axhard =df.getOWLSubClassOfAxiom(df.getOWLClass(DLClasses[dlInd]), hasComplexitySomeHard);
					
					manager.addAxiom(ontology, axhard);
					}
					else{
						//for inNExpTime
						OWLClassExpression hasComplexitySome =
								df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, innexptime);
						
						OWLSubClassOfAxiom ax =df.getOWLSubClassOfAxiom(df.getOWLClass(DLClasses[dlInd]), hasComplexitySome);
						
						manager.addAxiom(ontology, ax);
						
						//for NExpTimeHard
						OWLClassExpression hasComplexitySomeHard =
								df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, nexptimehard);
						
						OWLSubClassOfAxiom axhard =df.getOWLSubClassOfAxiom(df.getOWLClass(DLClasses[dlInd]), hasComplexitySomeHard);
						
						manager.addAxiom(ontology, axhard);
					}
				}
			}
		}



		//this is for fl0 dl
		Set<OWLClassExpression> firstDisj=new HashSet<OWLClassExpression>();
		firstDisj.add(atomicConcept);
		firstDisj.add(topConcept);
		firstDisj.add(bottomConcept);
		firstDisj.add(valueRestriction);
		firstDisj.add(conjunction);

		OWLClassExpression disjunctionOfConstructors =df.getOWLObjectUnionOf(firstDisj);

		OWLClassExpression onlyConstr=df.getOWLObjectAllValuesFrom(hasModelingFeatures, disjunctionOfConstructors);

		Set<OWLClassExpression> firstConj =new HashSet<OWLClassExpression>();
		firstConj.add(descriptionLogic);
		firstConj.add(onlyConstr);

		OWLClassExpression con=df.getOWLObjectIntersectionOf(firstConj);

		Set<OWLClassExpression> finalon=new HashSet<OWLClassExpression>();
		finalon.add(df.getOWLClass(DLClasses[80]));
		finalon.add(con);

		OWLAxiom finalFLZeroAxiom = df.getOWLEquivalentClassesAxiom(finalon);
		manager.addAxiom(ontology, finalFLZeroAxiom);

		//this is for FL-

		Set<OWLClassExpression> firstDisjFLMinus=new HashSet<OWLClassExpression>();
		firstDisjFLMinus.add(atomicConcept);
		firstDisjFLMinus.add(topConcept);
		firstDisjFLMinus.add(conjunction);
		firstDisjFLMinus.add(bottomConcept);
		firstDisjFLMinus.add(valueRestriction);
		firstDisjFLMinus.add(limitedExistentialRestriction);


		OWLClassExpression disjunctionOfConstructorsFLMinus =df.getOWLObjectUnionOf(firstDisjFLMinus);

		OWLClassExpression onlyConstrFLMinus=df.getOWLObjectAllValuesFrom(hasModelingFeatures, disjunctionOfConstructorsFLMinus);

		Set<OWLClassExpression> firstConjFLMinus =new HashSet<OWLClassExpression>();
		firstConjFLMinus.add(descriptionLogic);
		firstConjFLMinus.add(onlyConstrFLMinus);

		OWLClassExpression conFLMinus=df.getOWLObjectIntersectionOf(firstConjFLMinus);

		Set<OWLClassExpression> finalonFLMinus=new HashSet<OWLClassExpression>();
		finalonFLMinus.add(df.getOWLClass(DLClasses[81]));
		finalonFLMinus.add(conFLMinus);

		OWLAxiom finalFLMinusAxiom = df.getOWLEquivalentClassesAxiom(finalonFLMinus);
		manager.addAxiom(ontology, finalFLMinusAxiom);

		//this is for EL

		Set<OWLClassExpression> firstDisjEL=new HashSet<OWLClassExpression>();
		firstDisjEL.add(atomicConcept);
		firstDisjEL.add(topConcept);
		firstDisjEL.add(disjunction);
		firstDisjEL.add(existentialRestriction);


		OWLClassExpression disjunctionOfConstructorsEL =df.getOWLObjectUnionOf(firstDisjEL);

		OWLClassExpression onlyConstrEL=df.getOWLObjectAllValuesFrom(hasModelingFeatures, disjunctionOfConstructorsEL);

		Set<OWLClassExpression> firstConjEL =new HashSet<OWLClassExpression>();
		firstConjEL.add(descriptionLogic);
		firstConjEL.add(onlyConstrEL);

		OWLClassExpression conEL=df.getOWLObjectIntersectionOf(firstConjEL);

		Set<OWLClassExpression> finalonEL=new HashSet<OWLClassExpression>();
		finalonEL.add(df.getOWLClass(DLClasses[82]));
		finalonEL.add(conEL);

		OWLAxiom finalELAxiom = df.getOWLEquivalentClassesAxiom(finalonEL);
		manager.addAxiom(ontology, finalELAxiom);



		//this is for EL++

		Set<OWLClassExpression> firstDisjELPlus=new HashSet<OWLClassExpression>();
		firstDisjELPlus.add(atomicConcept);
		firstDisjELPlus.add(topConcept);
		firstDisjELPlus.add(bottomConcept);
		firstDisjELPlus.add(disjunction);
		firstDisjELPlus.add(existentialRestriction);


		OWLClassExpression disjunctionOfConstructorsELPlus =df.getOWLObjectUnionOf(firstDisjELPlus);

		OWLClassExpression onlyConstrELPlus=df.getOWLObjectAllValuesFrom(hasModelingFeatures, disjunctionOfConstructorsELPlus);

		Set<OWLClassExpression> firstConjELPlus =new HashSet<OWLClassExpression>();
		firstConjELPlus.add(descriptionLogic);
		firstConjELPlus.add(onlyConstrELPlus);

		OWLClassExpression conELPlus=df.getOWLObjectIntersectionOf(firstConjELPlus);

		Set<OWLClassExpression> finalonELPlus=new HashSet<OWLClassExpression>();
		finalonELPlus.add(df.getOWLClass(DLClasses[83]));
		finalonELPlus.add(conELPlus);

		OWLAxiom finalELPlusAxiom = df.getOWLEquivalentClassesAxiom(finalonELPlus);
		manager.addAxiom(ontology, finalELPlusAxiom);




		//this is for ALCH
		/*
       	Set<OWLClassExpression> firstDisjALCH=new HashSet<OWLClassExpression>();
       	firstDisjALCH.add(atomicConcept);
       	firstDisjALCH.add(conceptNegation);
       	firstDisjALCH.add(topConcept);
       	firstDisjALCH.add(bottomConcept);
       	firstDisjALCH.add(disjunction);
       	firstDisjALCH.add(valueRestriction);
       	firstDisjALCH.add(conjunction);
       	firstDisjALCH.add(existentialRestriction);
       	firstDisjALCH.add(roleInclusion);


       	OWLClassExpression disjunctionOfConstructorsALCH =df.getOWLObjectUnionOf(firstDisjALCH);

       	OWLClassExpression onlyConstrALCH=df.getOWLObjectAllValuesFrom(hasModelingFeatures, disjunctionOfConstructorsALCH);

       	Set<OWLClassExpression> firstConjALCH =new HashSet<OWLClassExpression>();
       	firstConjALCH.add(descriptionLogic);
       	firstConjALCH.add(onlyConstrALCH);

       	OWLClassExpression conALCH=df.getOWLObjectIntersectionOf(firstConjALCH);

       	Set<OWLClassExpression> finalonALCH=new HashSet<OWLClassExpression>();
       	finalonALCH.add(df.getOWLClass(DLClasses[1]));
       	finalonALCH.add(conALCH);

       	OWLAxiom finalALCHAxiom = df.getOWLEquivalentClassesAxiom(finalonALCH);
       	manager.addAxiom(ontology, finalALCHAxiom);



		 */



		//this is for SH

		/*

       	Set<OWLClassExpression> firstDisjSH=new HashSet<OWLClassExpression>();
       	firstDisjSH.add(atomicConcept);
       	firstDisjSH.add(conceptNegation);
       	firstDisjSH.add(topConcept);
       	firstDisjSH.add(bottomConcept);
       	firstDisjSH.add(disjunction);
       	firstDisjSH.add(valueRestriction);
       	firstDisjSH.add(conjunction);
       	firstDisjSH.add(existentialRestriction);
       	firstDisjSH.add(roleTransitivity);
       	firstDisjSH.add(roleInclusion);


       	OWLClassExpression disjunctionOfConstructorsSH =df.getOWLObjectUnionOf(firstDisjSH);

       	OWLClassExpression onlyConstrSH=df.getOWLObjectAllValuesFrom(hasModelingFeatures, disjunctionOfConstructorsSH);

       	Set<OWLClassExpression> firstConjSH =new HashSet<OWLClassExpression>();
       	firstConjSH.add(descriptionLogic);
       	firstConjSH.add(onlyConstrSH);

       	OWLClassExpression conSH=df.getOWLObjectIntersectionOf(firstConjSH);

       	Set<OWLClassExpression> finalonSH=new HashSet<OWLClassExpression>();
       	finalonSH.add(df.getOWLClass(DLClasses[3]));
       	finalonSH.add(conSH);

       	OWLAxiom finalSHAxiom = df.getOWLEquivalentClassesAxiom(finalonSH);
       	manager.addAxiom(ontology, finalSHAxiom);


		 */

		OWLClassExpression axiom=df.getOWLEntity(EntityType.CLASS, IRI.create("#Axiom"));
		OWLClassExpression aboxaxiom=df.getOWLEntity(EntityType.CLASS, IRI.create("#ABoxAxiom"));
		OWLClassExpression tboxaxiom=df.getOWLEntity(EntityType.CLASS, IRI.create("#TBoxAxiom"));
		OWLClassExpression rboxaxiom=df.getOWLEntity(EntityType.CLASS, IRI.create("#RBoxAxiom"));

		OWLAxiom axiomAxiom =df.getOWLSubClassOfAxiom(aboxaxiom, axiom);
		OWLAxiom axiomAxiom2 =df.getOWLSubClassOfAxiom(tboxaxiom, axiom);
		OWLAxiom axiomAxiom3 =df.getOWLSubClassOfAxiom(rboxaxiom, axiom);

		manager.addAxiom(ontology, axiomAxiom);
		manager.addAxiom(ontology, axiomAxiom2);
		manager.addAxiom(ontology, axiomAxiom3);


		OWLClassExpression box=df.getOWLEntity(EntityType.CLASS, IRI.create("#Box"));
		OWLClassExpression abox=df.getOWLEntity(EntityType.CLASS, IRI.create("#ABox"));
		OWLClassExpression tbox=df.getOWLEntity(EntityType.CLASS, IRI.create("#TBox"));
		OWLClassExpression rbox=df.getOWLEntity(EntityType.CLASS, IRI.create("#RBox"));

		OWLClassExpression acyclictbox=df.getOWLEntity(EntityType.CLASS, IRI.create("#AcyclicTBox"));
		OWLClassExpression emptytbox=df.getOWLEntity(EntityType.CLASS, IRI.create("#GeneralTBox"));
		OWLClassExpression generalbox=df.getOWLEntity(EntityType.CLASS, IRI.create("#EmptyTBox"));



		OWLAxiom boxAxiom =df.getOWLSubClassOfAxiom(abox, box);
		OWLAxiom boxAxiom2 =df.getOWLSubClassOfAxiom(tbox, box);
		OWLAxiom boxAxiom3 =df.getOWLSubClassOfAxiom(rbox, box);

		OWLAxiom boxAxiom4 =df.getOWLSubClassOfAxiom(acyclictbox, tbox);
		OWLAxiom boxAxiom5 =df.getOWLSubClassOfAxiom(emptytbox, tbox);
		OWLAxiom boxAxiom6 =df.getOWLSubClassOfAxiom(generalbox, tbox);

		manager.addAxiom(ontology, boxAxiom);
		manager.addAxiom(ontology, boxAxiom2);
		manager.addAxiom(ontology, boxAxiom3);
		manager.addAxiom(ontology, boxAxiom4);
		manager.addAxiom(ontology, boxAxiom5);
		manager.addAxiom(ontology, boxAxiom6);


		OWLClassExpression modelproperty=df.getOWLEntity(EntityType.CLASS, IRI.create("#ModelProperty"));
		OWLClassExpression finitemodel=df.getOWLEntity(EntityType.CLASS, IRI.create("#FiniteModelProperty"));
		OWLClassExpression treemodel=df.getOWLEntity(EntityType.CLASS, IRI.create("#TreeModelProperty"));

		OWLAxiom faxiom =df.getOWLSubClassOfAxiom(finitemodel, modelproperty);
		OWLAxiom taxiom =df.getOWLSubClassOfAxiom(treemodel, modelproperty);

		manager.addAxiom(ontology, faxiom);
		manager.addAxiom(ontology, taxiom);

		


		/*
		
		OWLObjectProperty forConceptSatIsIn= df.getOWLEntity(EntityType.OBJECT_PROPERTY, 
				IRI.create("#forConceptSatIsInCompCl"));
		OWLAxiom dlconc=df.getOWLDeclarationAxiom(forConceptSatIsIn);

		OWLObjectPropertyDomainAxiom domainAxiomdl = df.getOWLObjectPropertyDomainAxiom(forConceptSatIsIn, descriptionLogic);
		OWLObjectPropertyRangeAxiom   rangeAxiomcs = df.getOWLObjectPropertyRangeAxiom( forConceptSatIsIn, complexitycl);

		manager.addAxiom(ontology, dlconc);
		manager.addAxiom(ontology, domainAxiomdl);
		manager.addAxiom(ontology,  rangeAxiomcs);

		*/
		//adding the complexity
		
		/*
		
		OWLClassExpression hasComplexitySome =
				df.getOWLObjectSomeValuesFrom(forConceptSatIsIn, exptime);
			
		OWLSubClassOfAxiom ax =df.getOWLSubClassOfAxiom(finitemodel, hasComplexitySome);
		manager.addAxiom(ontology, ax);

		*/
		
		try {
			manager.saveOntology(ontology);
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Failed to save ontology!");
		}
		//Print the number of axioms in the ontology    
		System.out.println("Number of axioms: " + ontology.getAxiomCount());
		//for(int i=0;i<84;i++){
		// System.out.println(DLClasses[i]);
		//}

	}}
