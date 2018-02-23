
var modelData = [];
var salesData =[];
var hierarchyTypes = {"lineNumber": "line number", "product" : "product name" , "category" : "category name"}
function loadBackEndData(callback,weeks,hierarchyValue, startDate, hierarchyType){
    var datePath = ""
    if(hierarchyValue == ""){
        alert("Please enter the "+hierarchyTypes[hierarchyType])
    }else{
         if(startDate !== ""){
            var splittedDate = startDate.split("/")
            if(splittedDate.length == 3){
                datePath += "/" + splittedDate.reverse().join("-")
            }
        }
        $.ajax({
        dataType: 'json',
        headers: {
            'X-Hello': 'World',
            Accept:"application/json",
            "Access-Control-Allow-Origin": "*"
        },
        type:'GET',
        url:'http://localhost:8080/forecast/'+weeks+'/'+hierarchyType+'/'+hierarchyValue+datePath,
        success: function(data)
        {
            console.log(data)
            modelData.length = data.forecastings.length
            for (i = 0; i < data.forecastings.length; i++) { 
                var forecastingList = [];
                var foreCastingName = 'ForeCasting using '+data.forecastings[i].name;
                    for(x=0; x < data.forecastings[i].forecastedValues.length; x++){
                        forecastingList.push({x: getNewDate(data.forecastings[i].forecastedValues[x].date), 
                            y: data.forecastings[i].forecastedValues[x].stock});
                    }
                modelData[i] = {id: i+1, name: data.forecastings[i].name,
                    error: data.forecastings[i].error,
                    values:forecastingList
               };
             
            }
            var historyDataList = [];
            for(j=0;j<data.historicData.length;j++){
                    historyDataList.push({x: getNewDate(data.historicData[j].date), y: data.historicData[j].stock});
            }
            if(historyDataList.length > 0){
                salesData[0] = {id: i+1, name: 'Sales Data',
                    values:historyDataList};
            }else{
                salesData.length = 0
            }
            
                    
           callback();
        },
        error: function(data)
        {
            alert("error");
        }
    });
    }
}
    function getSalesData(){
        return salesData;
    }

function getNewDate(date){
    return new Date(date[0], date[1] - 1, date[2]);
}

function getHierarchyValue(id){
    return hierarchyTypes[id]
}

function getAvailableModels(){
   return modelData;
} 
function getModel(names) {
    /*var modelDetailsList = [];
    for (i = 0; i < modelData.length; i++) {
        for (x = 0; x < names.length; x++) {
            console.log("Comparing models ", modelData[i].name, names[x])
            if (modelData[i].name === names[x]) {
                modelDetailsList.push(modelData[i]);
            }
        }
    }
    console.log("Model Details List", modelDetailsList)
    return modelDetailsList;*/
    return modelData.filter(modelElem => names.indexOf(modelElem.name) !== -1)
}