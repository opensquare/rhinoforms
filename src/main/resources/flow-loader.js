function loadFlow(flowMap) {
	
	this.trim = function(string) {
		return string.replace(/^\s\s*/, '').replace(/\s\s*$/, '')
	}
	
	formFlow.setFlowDocBase(flowMap.docBase);
	var formLists = flowMap.formLists;
	for (formListName in formLists) {
		var formArray = formLists[formListName];
		var formListJ = new java.util.ArrayList();
		for (var formIndex in formArray) {
			var form = formArray[formIndex];
			var actionsJ = new java.util.HashMap();
			for (var actionIndex in form.actions) {
				var action = form.actions[actionIndex];
				var actionName;
				var actionTarget = "";
				var actionSubmission = null;
				if (action instanceof Object) {
					actionName = action.name;
					if (action.target) {
						actionTarget = action.target;
					}
					if (action.submission) {
						actionSubmission = action.submission;
					}
				} else {
					var actionParts = form.actions[actionIndex].split(':');
					actionName = actionParts[0];
					if (actionParts.length > 1) {
						actionTarget = actionParts[1];
					}
				}
				var actionTargetParamsIndex = actionTarget.indexOf("(");
				var actionTargetParamsParts;
				if (actionTargetParamsIndex != -1) {
					var actionTargetParamsString = actionTarget.substring(actionTargetParamsIndex + 1, actionTarget.indexOf(")"));
					actionTargetParamsParts = actionTargetParamsString.split(",");
					actionTarget = actionTarget.substring(0, actionTargetParamsIndex);
				}
				
				var flowActionJ = new com.rhinoforms.FlowAction(actionName, actionTarget);
				for (var actionTargetParamsPartIndex in actionTargetParamsParts) {
					var param = actionTargetParamsParts[actionTargetParamsPartIndex];
					var paramParts = param.split("=");
					flowActionJ.addParam(this.trim(paramParts[0]), this.trim(paramParts[1]));
				}
				if (actionSubmission) {
					var submissionJ = new com.rhinoforms.Submission(actionSubmission.url);
					submissionJ.setResultInsertPoint(actionSubmission.resultInsertPoint);
					submissionJ.setPreTransform(formFlow.resolveResourcePathIfRelative(actionSubmission.preTransform));
					submissionJ.setPostTransform(formFlow.resolveResourcePathIfRelative(actionSubmission.postTransform));
					flowActionJ.setSubmission(submissionJ);
				}
				actionsJ.put(actionName, flowActionJ);
			}
			var formJ = new com.rhinoforms.Form(form.id, form.url, actionsJ, formIndex);
			if (form.docBase) {
				formJ.setDocBase(form.docBase);
			}
			formListJ.add(formJ);
		}
		formFlow.addFormList(formListName, formListJ);
	}
	
}