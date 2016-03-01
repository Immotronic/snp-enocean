var EnoceanAddingUIHandler = 
{
	UID: "fr.immotronic.ubikit.pems.enocean",
	baseURL: "/Immotronic/enocean/",
	
	init: function()
	{
		Placetouch.registerPemAddingUIHandler(EnoceanAddingUIHandler.UID, EnoceanAddingUIHandler);
		EnoceanAddingUIHandler.__loadAddingUI();
	},
	
	addAddingUI: function(sel)
	{ 
		if(EnoceanAddingUIHandler.__UI != undefined)
		{
			$(sel).append(EnoceanAddingUIHandler.__UI);
			EnoceanAddingUIHandler.__initUI(sel);
		}
	},
	
	__initUI: function(sel)
	{	
		cwf.l10n.tr_element(sel);
		
		$("#EnoceanAddNode").click(function() {
			cwf.api.query(EnoceanAddingUIHandler.baseURL+"api/add", {
				actuatorProfile: $(sel+" #actuatorProfile").val(),
				customName: $(sel+" #itemName").val(),
				location: $(sel+" #itemLocation").val()
			},
			{
				success: function(data)
				{
					$(sel+" #addingLogicalActuator").hide();
					$(sel+" #pairingActuator").show();
					$(sel+" #itemName").val("");
					$(sel+" #actuatorUID").val(data.actuatorUID);
				}
			},
			{
				to_hide: ["#EnoceanAddNode"],
				to_show: [sel+" IMG"]
			});
		});
		
		$("#EnoceanPairAcutatorButton").click(function() {
			cwf.api.query(EnoceanAddingUIHandler.baseURL+"api/pair", {
				actuatorUID: $(sel+" #actuatorUID").val()
			});
		});
		
		$("#EnoceanAddingDoneButton").click(function() {
			$(sel+" #addingLogicalActuator").show();
			$(sel+" #pairingActuator").hide();
		});
	},
	
	__loadAddingUI: function()
	{
		$.ajax({	
			type: "GET", 
			url:EnoceanAddingUIHandler.baseURL + "ui/adding/"+EnoceanAddingUIHandler.__sysappName+"/adding.html",
			success: function(data) {
				EnoceanAddingUIHandler.__UI = data;
			},
			error: function() { 
				cwf.log("No adding UI in PEM "+EnoceanAddingUIHandler.UID);
			}
		});
	},
	
	__UI: {},
	
	__sysappName : "greenbox"
}

EnoceanAddingUIHandler.init();