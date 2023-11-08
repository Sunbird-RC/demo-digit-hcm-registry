import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { environment } from 'src/environments/environment';

import {
    Pipe,
    PipeTransform,
    OnDestroy,
    WrappedValue,
    ChangeDetectorRef
} from '@angular/core';

import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { GeneralService } from 'src/app/services/general/general.service';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { KeycloakService } from 'keycloak-angular';
import { AppConfig } from 'src/app/app.config';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { pdfDefaultOptions } from 'ngx-extended-pdf-viewer';


@Component({
    selector: 'app-doc-view',
    templateUrl: './doc-view.component.html',
    styleUrls: ['./doc-view.component.css']
})
export class DocViewComponent implements OnInit {
    docUrl: string;
    baseUrl = this.config.getEnv('baseUrl');
    extension;
    token
    public bearerToken: string | undefined = undefined;
    id: any;
    excludedFields: any = ['osid','id', 'type','fileUrl','otp','transactionId'];
    document = [];
    loader: boolean = true;
  docName: any;
  docDetails: any;
    constructor(private route: ActivatedRoute, public generalService: GeneralService,
        private keycloakService: KeycloakService, private config: AppConfig,private router: Router) {
        this.token = this.keycloakService.getToken();
        pdfDefaultOptions.renderInteractiveForms = false;
    }

    ngOnInit(): void {
        this.route.params.subscribe(async params => {
            if(params.id && params.type){
                this.id = params.id;
                this.generalService.getData(params.entity).subscribe((res) => {
                  let fileUrls = res[0]["attestation-SELF"]
                  ?.find(d => d.osid === params.id)
                  ?.additionalInput?.fileUrl;
                  if (!!fileUrls && Array.isArray(fileUrls)) {
                    fileUrls.forEach(doc => {
                      var tempObject = {}
                      console.log("element", doc)
                      tempObject['name'] = doc.split('-').slice(-1)[0];
                      tempObject['type'] = res['name'];
                      tempObject['file'] =  this.baseUrl + '/' + doc;
                      tempObject['extension'] = doc.split('.').slice(-1)[0];
                      tempObject['osid'] = res['osid'];
                      // this.docUrl = this.baseUrl + '/' + doc;
                      this.extension = doc.split('.').slice(-1)[0];
                      this.document.push(tempObject);
                      this.generalService.getDocument(this.baseUrl + '/' + doc).subscribe((pdf) => {
                        console.log("doc url: ", pdf, this.extension);
                        if(["text", "txt", "html", "xhtml", "json"].includes(this.extension)) {
                          let reader = new FileReader();
                          reader.addEventListener('loadend', (e) => {
                            this.docUrl = e.target.result.toString();
                            console.log(this.docUrl);
                          });
                          reader.readAsText(pdf)
                          console.log("file after reading as text: ", reader.result);
                          // this.docUrl = pdf;
                        } else {
                          this.docUrl = URL.createObjectURL(pdf);
                        }
                        this.loader = false;
                      });
                    });
                    console.log("added files to document: ", this.document);
                  } else {
                    console.log("invalid file urls: ", fileUrls, typeof fileUrls)
                  }
                });
                // this.generalService.getData(params.type+'/'+params.id).subscribe((res) => {
                //     console.log('pub res', res);
                //     if(res.name == 'attestation-SELF'){
                //       this.docName = res['additionalInput'].name;
                //         for (const [key, value] of Object.entries(res['additionalInput'])) {
                //             var tempObject = {}
                //             if(key === 'fileUrl'){
                //                 this.docUrl = this.baseUrl + '/' + value;
                //                 this.extension = this.docUrl.split('.').slice(-1)[0];
                //             }
                //             if (typeof value != 'object') {
                //               if (!this.excludedFields.includes(key)) {
                //                 tempObject['key'] = key;
                //                 tempObject['value'] = value;
                //                 tempObject['type'] = res['name'];
                //                 tempObject['osid'] = res['osid'];
                //                 if(res['logoUrl']){
                //                   tempObject['logoUrl'] = res['logoUrl']
                //                 }
                //                 this.document.push(tempObject);
                //               }
                //             } else {
                //               if (!this.excludedFields.includes(key)) {
                //                 tempObject['key'] = key;
                //                 tempObject['value'] = value[0];
                //                 tempObject['type'] = res['name'];
                //                 tempObject['osid'] = res['osid'];
                //                 if(res['logoUrl']){
                //                   tempObject['logoUrl'] = res['logoUrl']
                //                 }
                //                 this.document.push(tempObject);
                //               }
                //             } 
                //           }
                //           this.loader = false;
                //     }else if (res.name == 'attestation-DIVOC')
                //     {
                    
                //       this.docDetails = (JSON.parse(res['additionalInput'])).signedCredentials.credentialSubject;
                //       this.docName = this.docDetails.name;
                //       console.log(this.docDetails);
            
                //       this.loader = false;
                //       let _self = this;
                //         Object.keys( this.docDetails).forEach(function (key) {
                //           var temp_object : any = {};
                //           if (_self.docDetails[key] != undefined && typeof _self.docDetails[key] != 'object') {
                //         console.log({key});
                //          temp_object['title'] = key;
                //          temp_object['value'] = _self.docDetails[key];
                //         _self.document.push(temp_object);
                //           }
                //       });
                  
            
            
                //     }else{
                //         if(res['_osAttestedData'] && JSON.parse(res['_osAttestedData'])['files']){
                //             this.docUrl = this.baseUrl + '/' + JSON.parse(res['_osAttestedData'])['files'][0];
                //             this.extension = this.docUrl.split('.').slice(-1)[0];
                //         }
                //         for (const [key, value] of Object.entries(res['additionalInput'])) {
                //             var tempObject = {}
                //             if(key === 'fileUrl'){
                //                 this.docUrl = this.baseUrl + '/' + value;
                //                 this.extension = this.docUrl.split('.').slice(-1)[0];
                //             }
                //             if (typeof value != 'object') {
                //               if (!this.excludedFields.includes(key)) {
                //                 tempObject['key'] = key;
                //                 tempObject['value'] = value;
                //                 tempObject['type'] = res['name'];
                //                 tempObject['osid'] = res['osid'];
                //                 if(res['logoUrl']){
                //                   tempObject['logoUrl'] = res['logoUrl']
                //                 }
                //                 this.document.push(tempObject);
                //               }
                //             } else {
                //               if (!this.excludedFields.includes(key)) {
                //                 tempObject['key'] = key;
                //                 tempObject['value'] = value[0];
                //                 tempObject['type'] = res['name'];
                //                 tempObject['osid'] = res['osid'];
                //                 if(res['logoUrl']){
                //                   tempObject['logoUrl'] = res['logoUrl']
                //                 }
                //                 this.document.push(tempObject);
                //               }
                //             }
    
                            
                //           }
                //           this.loader = false;
                //     }
                //       console.log('this.document',this.document)
                //   }, (err) => {
                //     // this.toastMsg.error('error', err.error.params.errmsg)
                //     console.log('error', err)
                //   });
                  
            }
           
        })
    }

    goBack(){
      this.router.navigateByUrl('');
    }
}


// Using similarity from AsyncPipe to avoid having to pipe |secure|async in HTML.

@Pipe({
    name: 'authImage'
})
export class AuthImagePipe implements PipeTransform {
    extension;
    constructor(
        private http: HttpClient, private route: ActivatedRoute,
        private keycloakService: KeycloakService, // our service that provides us with the authorization token
    ) {

        // this.route.queryParams.subscribe(async params => {
        //     this.extension = params.u.split('.').slice(-1)[0];
        // })
    }

    async transform(src: string,extension:string): Promise<any> {
        this.extension = extension;
        const token = this.keycloakService.getToken();
        const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });
        let imageBlob = await this.http.get(src, { headers, responseType: 'blob' }).toPromise();

        if (this.extension == 'pdf') {
            imageBlob = new Blob([imageBlob], { type: 'application/' + this.extension })
        } else {
            imageBlob = new Blob([imageBlob], { type: 'image/' + this.extension })
        }

        const reader = new FileReader();
        return new Promise((resolve, reject) => {
            reader.onloadend = () => resolve(reader.result as string);
            reader.readAsDataURL(imageBlob);
        });
    }

}


