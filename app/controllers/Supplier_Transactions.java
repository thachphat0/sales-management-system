package controllers;

import models.Supplier;
import models.Product;
import models.Supplier_Transaction;
import play.mvc.Controller;
import play.mvc.Result;
import play.data.Form;
import views.html.supplier_transactions.update;
import views.html.supplier_transactions.list;
import views.html.supplier_transactions.details;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import models.User_Action;

public class Supplier_Transactions extends Controller {
	private static final Form<Supplier_Transaction> transactionForm = Form.form(Supplier_Transaction.class);

	public static Result newTransaction(Long id){
		return ok(update.render(transactionForm,id));
	}
	//List all transactions
	public static Result list(){
		List<Supplier_Transaction> transactions = Supplier_Transaction.find.all();
		return ok(list.render(transactions));
	}
	//Update transaction with transactionID
	public static Result details(Long id){
		Supplier_Transaction transaction = Supplier_Transaction.find.byId(id);
		if(transaction==null){
			return notFound("Transaction ID "+id+" not found.");
		}
		Form<Supplier_Transaction> filledForm = transactionForm.fill(transaction);
		return ok(update.render(filledForm,transaction.supplier.id));
	}
	//Save to supplierID
	public static Result save(Long id){
		Form<Supplier_Transaction> filledForm = transactionForm.bindFromRequest();
		if(filledForm.hasErrors()){
			flash("error","Please correct the form below.");
			return badRequest(update.render(transactionForm, id));
		}
		Supplier_Transaction transaction = filledForm.get();
		List<Product> products = Product.findByEan(filledForm.apply("product_ean").value());
		if (products.isEmpty()){
			flash("error","Product is not found.");
			return badRequest(update.render(filledForm,id));
		}
		if (transaction.quantity==0){
			flash("error","Please input quantity.");
			return badRequest(update.render(filledForm,id));
		}
		if (transaction.price==0){
			flash("error","Please input price.");
			return badRequest(update.render(filledForm,id));
		}
		if (transaction.buyDate==null){
			flash("error","Please input date.");
			return badRequest(update.render(filledForm,id));
		}
		/*if (transaction.isPaid==null){
			flash("error","Please input status.");
			return badRequest(update.render(filledForm,id));
		}*/
		/*SimpleDateFormat format = new SimpleDateFormat("mm/dd/yy");
		String dateString = requestData.get("buyDate");
		try{
			transaction.buyDate = format.parse(dateString);
		}
		catch (java.text.ParseException e){

		}*/
		Product product = products.get(0);
		User_Action action = new User_Action();
		if (transaction.internalId==null) {
			Supplier supplier = Supplier.findByID(id);
			transaction.supplier = supplier;
			transaction.save();
			String str = String.format("Transaction %s : Imported %s to product %s-%s from %s",transaction.internalId,transaction.quantity, product.id, product.name, transaction.supplier.name);
			action.verb= "Insert";
			action.description=str;
			action.save();
			product.setInstock(product.instock + transaction.quantity);
			transaction.product = product;
			product.update();
		}else{
			Supplier_Transaction oldTransaction = Supplier_Transaction.find.byId(transaction.internalId);
			String str = String.format("Transaction %s : Updated import quantity from %s to %s to product %s-%s from %s",transaction.internalId,oldTransaction.quantity,transaction.quantity, product.id, product.name, oldTransaction.supplier.name);
			action.verb= "Update";
			action.description=str;
			action.save();
			product.setInstock(product.instock + transaction.quantity - oldTransaction.quantity);
			product.update();
			transaction.update();
		}

		return redirect(routes.Suppliers.details(id));
	}
	public static Result delete(Long id){
		Supplier_Transaction transaction = Supplier_Transaction.find.byId(id);
		if (transaction==null){
			return notFound(String.format("Transaction %s does not exist.", id));
		}

		Product product = transaction.product;
		product.setInstock(product.instock - transaction.quantity);
		product.update();

		transaction.delete();
		User_Action action = new User_Action();
		String str = String.format("Transaction %s : Remove %s of product %s-%s from %s",transaction.internalId,transaction.quantity, product.id, product.name, transaction.supplier.name);
		action.verb= "Delete";
		action.description=str;
		action.save();
		return redirect(routes.Supplier_Transactions.list());
	}
}
