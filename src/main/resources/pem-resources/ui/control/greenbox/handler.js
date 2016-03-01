var EnoceanControlUIHandler = 
{
	UID: "fr.immotronic.ubikit.pems.enocean",
	baseURL: "/Immotronic/enocean/",
	
	init: function()
	{
		Placetouch.registerPemControlUIHandler(EnoceanControlUIHandler.UID, EnoceanControlUIHandler);
		EnoceanControlUIHandler.__loadCapabilityUIs();
	},
		
	addControlUI: function(itemUID, capability, sel)
	{
		var ui = EnoceanControlUIHandler.__capabilityUIs[capability]; 
		if(ui != undefined)
		{
			$(sel).append(ui);
			EnoceanControlUIHandler.__initUI(itemUID, capability);
		}
	},
	
	doesControlUIExistFor: function(capability)
	{
		return EnoceanControlUIHandler.__capabilityUIs[capability] != undefined;
	},
	
	__initUI: function(itemUID, capability)
	{
		switch(capability)
		{
			case "ONOFF_DEVICE":
				$("#Enocean_onButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/on/"+itemUID, {});
				});
				
				$("#Enocean_offButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/off/"+itemUID, {});
				});
				
				break;
				
			case "BLIND_AND_SHUTTER_MOTOR_DEVICE":
				
				cwf.api.get_query(EnoceanControlUIHandler.baseURL+"api/isInverted/"+itemUID, {
					success: function(data)
					{
						if(data.inverted) {
							$("#Enocean_invertMotorCheckbox").attr("checked", "checked");
						}
					}
				});
				
				$("#Enocean_upButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/up/"+itemUID, {});
				});
				
				$("#Enocean_stepUpButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/stepUp/"+itemUID, {});
				});
				
				$("#Enocean_stopButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/stop/"+itemUID, {});
				});
				
				$("#Enocean_stepDownButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/stepDown/"+itemUID, {});
				});
				
				$("#Enocean_downButton").click(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/down/"+itemUID, {});
				});
				
				$("#Enocean_invertMotorCheckbox").change(function() {
					cwf.api.query(EnoceanControlUIHandler.baseURL+"api/invertMotor/"+itemUID, { invert: $("#Enocean_invertMotorCheckbox").attr("checked") } );
				});
				
				break;
		}
		
		$("#EnoceanPairAcutatorButton").click(function() {
			cwf.api.query(EnoceanControlUIHandler.baseURL+"api/pair", {
				actuatorUID: itemUID
			});
		});
	},
	
	__loadCapabilityUIs: function()
	{
		EnoceanControlUIHandler.__loadCapabilityUI("ONOFF_DEVICE");
		EnoceanControlUIHandler.__loadCapabilityUI("BLIND_AND_SHUTTER_MOTOR_DEVICE");
	},
	
	__loadCapabilityUI: function(capability)
	{
		$.ajax({	
			type: "GET", 
			url:EnoceanControlUIHandler.baseURL + "ui/control/placetouch/"+capability+".html",
			success: function(data) {
				EnoceanControlUIHandler.__capabilityUIs[capability] = data;
			},
			error: function() { 
				cwf.log("No control UI for "+capability+" capability in PEM "+EnoceanControlUIHandler.UID);
			}
		});
	},
	
	__capabilityUIs: {}
}

EnoceanControlUIHandler.init();