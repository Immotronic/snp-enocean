var EnoceanConfigurationUIHandler = 
{
	UID: "fr.immotronic.ubikit.pems.enocean",
	baseURL: "/Immotronic/enocean/",
	
	init: function()
	{
		Placetouch.registerPemConfigurationUIHandler(EnoceanConfigurationUIHandler.UID, EnoceanConfigurationUIHandler);
		EnoceanConfigurationUIHandler.__loadConfigurationUI();
	},
	
	addConfigurationUI: function(sel)
	{ 
		if(EnoceanConfigurationUIHandler.__UI != undefined)
		{
			$(sel).append(EnoceanConfigurationUIHandler.__UI);
			EnoceanConfigurationUIHandler.__initUI(sel);
		}
	},
	
	__initUI: function(sel)
	{	
		cwf.l10n.tr_element(sel);
		
		$("#WriteNewTcmBaseIDButton").click(function() {
			cwf.api.query(EnoceanConfigurationUIHandler.baseURL + "api/writetcmbaseid", { tcmBaseID: $("INPUT#tcmBaseId").val() }, {
				success: function(data)
				{
					
				},
				
				failed: function(reason, code)
				{
					alert("reason="+reason+", code="+code);
				}
			});
		});
		
		cwf.api.get_query(EnoceanConfigurationUIHandler.baseURL + "api/gettcmbaseid", {
			success: function(data)
			{
				$("INPUT#tcmBaseId").val(data.tcmBaseID);
			}
		});
	},
	
	__loadConfigurationUI: function()
	{
		$.ajax({	
			type: "GET", 
			url:EnoceanConfigurationUIHandler.baseURL + "ui/configuration/"+EnoceanConfigurationUIHandler.__sysappName+"/configuration.html",
			success: function(data) {
				EnoceanConfigurationUIHandler.__UI = data;
			},
			error: function() { 
				cwf.log("No configuration UI in PEM "+EnoceanConfigurationUIHandler.UID);
			}
		});
	},
	
	__UI: {},
	
	__sysappName : "placetouch"
}

EnoceanConfigurationUIHandler.init();